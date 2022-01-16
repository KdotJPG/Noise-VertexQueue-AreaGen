/*
 * SuperSimplex Noise 2D Area Generation metrics.
 * Modified to also compare against FastNoise.
 */

class NoiseMetricsVsFastNoise {
	
	static final int N_INSTANCES = 1;
	static final int N_PREP_ITERATIONS = 8;
	static final int N_TIMED_ITERATIONS = 16;
	
	static final int WIDTH = 1024;
	static final int HEIGHT = 1024;
	static final double NOISE_EVAL_PERIOD = 128.0;
	static final int OFF_X = 8192;
	static final int OFF_Y = 8192;
	static final FastNoise.NoiseType fastNoiseType = FastNoise.NoiseType.Simplex;
	
	static final double NOISE_EVAL_FREQ = 1.0 / NOISE_EVAL_PERIOD;
	
	public static void main(String[] args) {
		
		OpenSimplex2S.GenerateContext2D ctx = new OpenSimplex2S.GenerateContext2D(OpenSimplex2S.LatticeOrientation2D.Standard, NOISE_EVAL_FREQ, NOISE_EVAL_FREQ, 1.0);
		OpenSimplex2S[] noises = new OpenSimplex2S[N_INSTANCES];
		FastNoise[] fastNoises = new FastNoise[N_INSTANCES];
		for (int i = 0; i < N_INSTANCES; i++) {
			noises[i] = new OpenSimplex2S(i);
			fastNoises[i] = new FastNoise(i);
			fastNoises[i].SetNoiseType(fastNoiseType);
			fastNoises[i].SetFrequency((float)NOISE_EVAL_FREQ);
		}
		
		long time1 = 0;
		double sum1 = 0;
		long time2 = 0;
		double sum2 = 0;
		long time3 = 0;
		double sum3 = 0;

		for (int ie = 0; ie < N_PREP_ITERATIONS + N_TIMED_ITERATIONS; ie++) {
			
			for (int i = 0; i < N_INSTANCES; i++) {
				
				double[][] buffer = new double[HEIGHT][WIDTH];
				long start = System.currentTimeMillis();
				
				// Generate area
				noises[i].generate2(ctx, buffer, OFF_X, OFF_Y);
				
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
						buffer[y][x] = noises[i].noise2((x + OFF_X) * NOISE_EVAL_FREQ, (y + OFF_Y) * NOISE_EVAL_FREQ);
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

		for (int ie = 0; ie < N_PREP_ITERATIONS + N_TIMED_ITERATIONS; ie++) {
			
			for (int i = 0; i < N_INSTANCES; i++) {
				
				double[][] buffer = new double[HEIGHT][WIDTH];
				long start = System.currentTimeMillis();
				
				// Generate traditionally
				for (int y = 0; y < HEIGHT; y++) {
					for (int x = 0; x < WIDTH; x++) {
						buffer[y][x] = fastNoises[i].GetNoise((x + OFF_X), (y + OFF_Y));
					}
				}
				
				long elapsed = System.currentTimeMillis() - start;
				
				// Want to make sure the JVM isn't taking any shortcuts for unused values (would it do that?)
				for (int y = 0; y < HEIGHT; y++) {
					for (int x = 0; x < WIDTH; x++) {
						sum3 += buffer[y][x];
					}
				}
			
				if (ie >= N_PREP_ITERATIONS) {
					time3 += elapsed;
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
		System.out.println("---- FastNoise " + fastNoiseType + " 2D Evaluation ----");
		System.out.println("Sum of all noise values: " + sum3 + " (sanity check)");
		System.out.println("Total milliseconds: " + time3);
		System.out.println("Nanoseconds per generated value: " + (time3 * 1_000_000.0 / (N_TIMED_ITERATIONS * N_INSTANCES * WIDTH * HEIGHT)));
		System.out.println();
		System.out.println("---- Area vs Traditional (non-FastNoise) ----");
		System.out.println("Performance ratio: ~" + Math.round(time2 * 100.0 / time1) + "%");
		System.out.println("Time ratio: ~" + Math.round(time1 * 100.0 / time2) + "%");
		System.out.println();
		System.out.println("---- Area vs FastNoise " + fastNoiseType + " 2D ----");
		System.out.println("Performance ratio: ~" + Math.round(time3 * 100.0 / time1) + "%");
		System.out.println("Time ratio: ~" + Math.round(time1 * 100.0 / time3) + "%");
		
		
	}
	
	
}