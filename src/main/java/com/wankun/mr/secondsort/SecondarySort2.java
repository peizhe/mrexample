package com.wankun.mr.secondsort;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URI;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.RawComparator;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * 二次排序关键是在map完数据传递给reduce的时候如何比较key值 groupingComparator分组器的使用
 * @author wankun
 *
 */
public class SecondarySort2 {

	public static String INPUT_PATH = "/tmp/input3";
	public static String OUTPUT_PATH = "/tmp/output3";
	

	/**
	 * 在分组比较的时候，只比较原来的key，而不是组合key。
	 */
	public static class GroupingComparator implements RawComparator<IntPair> {
		@Override
		public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
			return WritableComparator.compareBytes(b1, s1, Integer.SIZE / 8, b2, s2, Integer.SIZE / 8);
		}

		@Override
		public int compare(IntPair o1, IntPair o2) {
			int first1 = o1.getFirst();
			int first2 = o2.getFirst();
			return first1 - first2;
		}
	}

	public static class Map extends Mapper<LongWritable, Text, IntPair, IntWritable> {

		private final IntPair key = new IntPair();
		private final IntWritable value = new IntWritable();

		@Override
		public void map(LongWritable inKey, Text inValue, Context context) throws IOException, InterruptedException {
			StringTokenizer itr = new StringTokenizer(inValue.toString(),":");
			int left = 0;
			int right = 0;
			if (itr.hasMoreTokens()) {
				left = Integer.parseInt(itr.nextToken());
				if (itr.hasMoreTokens()) {
					right = Integer.parseInt(itr.nextToken());
				}
				key.set(left, right);
				value.set(right);
				context.write(key, value);
			}
		}
	}

	public static class Reduce extends Reducer<IntPair, IntWritable, Text, IntWritable> {
		private static final Text SEPARATOR = new Text("-------------------------");
		private final Text first = new Text();
		private IntWritable DEFAULT=new IntWritable();

		@Override
		public void reduce(IntPair key, Iterable<IntWritable> values, Context context) throws IOException,
				InterruptedException {
			context.write(SEPARATOR, DEFAULT);
			first.set(Integer.toString(key.getFirst()));
			for (IntWritable value : values) {
				context.write(first, value);
			}
		}
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();

		final FileSystem fileSystem = FileSystem.get(new URI("hdfs://mycluster"), conf);
		fileSystem.delete(new Path(OUTPUT_PATH), true);

		Job job = new Job(conf, "secondary sort");
		job.setJarByClass(SecondarySort2.class);
		job.setMapperClass(Map.class);
		job.setReducerClass(Reduce.class);

		job.setGroupingComparatorClass(GroupingComparator.class);

		job.setMapOutputKeyClass(IntPair.class);
		job.setMapOutputValueClass(IntWritable.class);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);

		FileInputFormat.addInputPath(job, new Path(INPUT_PATH));
		FileOutputFormat.setOutputPath(job, new Path(OUTPUT_PATH));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}

	/**
	 * 把第一列整数和第二列作为类的属性，并且实现WritableComparable接口
	 */
	public static class IntPair implements WritableComparable<IntPair> {
		private int first = 0;
		private int second = 0;

		public void set(int left, int right) {
			first = left;
			second = right;
		}

		public int getFirst() {
			return first;
		}

		public int getSecond() {
			return second;
		}

		@Override
		public void readFields(DataInput in) throws IOException {
			first = in.readInt();
			second = in.readInt();
		}

		@Override
		public void write(DataOutput out) throws IOException {
			out.writeInt(first);
			out.writeInt(second);
		}

		@Override
		public int hashCode() {
			return first + "".hashCode() + second + "".hashCode();
		}

		@Override
		public boolean equals(Object right) {
			if (right instanceof IntPair) {
				IntPair r = (IntPair) right;
				return r.first == first && r.second == second;
			} else {
				return false;
			}
		}

		// 这里的代码是关键，因为对key排序时，调用的就是这个compareTo方法
		@Override
		public int compareTo(IntPair o) {
			if (first != o.first) {
				return first - o.first;
			} else if (second != o.second) {
				return -(second - o.second);
			} else {
				return 0;
			}
		}
	}
}
