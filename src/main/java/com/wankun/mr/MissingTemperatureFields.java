package com.wankun.mr;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.mapred.Counters;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobID;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.Task;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class MissingTemperatureFields extends Configured implements Tool {
	@Override
	public int run(String[] args) throws Exception {
		if (args.length != 1) {
			JobBuilder.printUsage(this, "<job ID>");
			return -1;
		}
		String jobID = args[0];
		JobClient jobClient = new JobClient(new JobConf(getConf()));
		RunningJob job = jobClient.getJob(JobID.forName(jobID));
		if (job == null) {
			System.err.printf("No job with ID %s found.\n", jobID);
			return -1;
		}
		if (!job.isComplete()) {
			System.err.printf("Job %s is not complete.\n", jobID);
			return -1;
		}
		Counters counters = job.getCounters();
		long missing = counters.getCounter(MaxTemperatureWithCounters.Temperature.MISSING);
		long total = counters.getCounter(Task.Counter.MAP_INPUT_RECORDS);
		System.out.printf("Records with missing temperature fields: %.2f%%\n", 100.0 * missing / total);
		return 0;
	}

	public static void main(String[] args) throws Exception {
		int exitCode = ToolRunner.run(new MissingTemperatureFields(), args);
		System.exit(exitCode);
	}
}