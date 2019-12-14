/*
 * SuperSimplex Noise 2D Area Generation metrics.
 */

class NoiseMetrics {
	
	static final int N_INSTANCES = 1;
	static final int N_PREP_ITERATIONS = 8;
	static final int N_TIMED_ITERATIONS = 16;
	
	static final int WIDTH = 512;
	static final int HEIGHT = 512;
	static final double NOISE_EVAL_PERIOD = 128.0;
	static final int OFF_X = 8192;
	static final int OFF_Y = 8192;
	
	static final double NOISE_EVAL_FREQ = 1.0 / NOISE_EVAL_PERIOD;
	
	public static void main(String[] args) {
		
		SuperSimplex2DAreaGen.GenerateContext2D ctx = new SuperSimplex2DAreaGen.GenerateContext2D(NOISE_EVAL_FREQ);
		SuperSimplex2DAreaGen[] noises = new SuperSimplex2DAreaGen[N_INSTANCES];
		for (int i = 0; i < N_INSTANCES; i++) {
			noises[i] = new SuperSimplex2DAreaGen(i);
		}
		
		long time1 = 0;
		double sum1 = 0;
		long time2 = 0;
		double sum2 = 0;

		for (int ie = 0; ie < N_PREP_ITERATIONS + N_TIMED_ITERATIONS; ie++) {
			
			for (int i = 0; i < N_INSTANCES; i++) {
				
				double[][] buffer = new double[HEIGHT][WIDTH];
				long start = System.currentTimeMillis();
				
				// Generate area
				noises[i].generate(ctx, buffer, OFF_X, OFF_Y);
				
				long elapsed = System.currentTimeMillis() - start;
				
				// Want to make sure the JVM isn't taking any shortcuts for unused values (would it do that?)
				for (int y = 0; y < HEIGHT; y++) {
					for (int x = 0; x < WIDTH; x++) {
						sum1 += buffer[y][x];
					}
				}
			
				if (ie >= N_PREP_ITERATIONS) {
					time1 += elapsed;
				}
			}
		}

		for (int ie = 0; ie < N_PREP_ITERATIONS + N_TIMED_ITERATIONS; ie++) {
			
			for (int i = 0; i < N_INSTANCES; i++) {
				
				double[][] buffer = new double[HEIGHT][WIDTH];
				long start = System.currentTimeMillis();
				
				// Generate traditionally
				for (int y = 0; y < HEIGHT; y++) {
					for (int x = 0; x < WIDTH; x++) {
						buffer[y][x] = noises[i].eval((x + OFF_X) * NOISE_EVAL_FREQ, (y + OFF_Y) * NOISE_EVAL_FREQ);
					}
				}
				
				long elapsed = System.currentTimeMillis() - start;
				
				// Want to make sure the JVM isn't taking any shortcuts for unused values (would it do that?)
				for (int y = 0; y < HEIGHT; y++) {
					for (int x = 0; x < WIDTH; x++) {
						sum2 += buffer[y][x];
					}
				}
			
				if (ie >= N_PREP_ITERATIONS) {
					time2 += elapsed;
				}
			}
		}
		
		System.out.println("Noise class name: " + noises[0].getClass().getName());
		System.out.println("Number of prep iterations: " + N_PREP_ITERATIONS);
		System.out.println("Number of timed iterations: " + N_TIMED_ITERATIONS);
		System.out.println("Size: " + WIDTH  + "x" + HEIGHT);
		System.out.println("Offset: " + OFF_X  + "," + OFF_Y);
		System.out.println("Noise Period: " + NOISE_EVAL_PERIOD);
		System.out.println();
		System.out.println("---- Area Generation ----");
		System.out.println("Sum of all noise values: " + sum1 + " (sanity check)");
		System.out.println("Total milliseconds: " + time1);
		System.out.println("Nanoseconds per generated value: " + (time1 * 1_000_000.0 / (N_TIMED_ITERATIONS * N_INSTANCES * WIDTH * HEIGHT)));
		System.out.println();
		System.out.println("---- Traditional Evaluation ----");
		System.out.println("Sum of all noise values: " + sum2 + " (sanity check)");
		System.out.println("Total milliseconds: " + time2);
		System.out.println("Nanoseconds per generated value: " + (time2 * 1_000_000.0 / (N_TIMED_ITERATIONS * N_INSTANCES * WIDTH * HEIGHT)));
		System.out.println();
		System.out.println("Performance ratio: ~" + Math.round(time2 * 100.0 / time1) + "%");
		System.out.println("Time ratio: ~" + Math.round(time1 * 100.0 / time2) + "%");
		
		
	}
	
	
}