/**
 * Proof-of-concept speed-optimized gradient noise whole-area generator.
 * Uses a vertex queue, that propagates neighbor-to-neighbor.
 * Does not use a "range".
 *
 * @author K.jpg
 * 
 * End Software Patents: http://endsoftpatents.org/
 */
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;

public class SuperSimplex2DAreaGen {

	private short[] perm;
	private short[] perm2D;

	public SuperSimplex2DAreaGen(long seed) {
		perm = new short[1024];
		perm2D = new short[1024];
		short[] source = new short[1024]; 
		for (short i = 0; i < 1024; i++)
			source[i] = i;
		for (int i = 1023; i >= 0; i--) {
			seed = seed * 6364136223846793005L + 1442695040888963407L;
			int r = (int)((seed + 31) % (i + 1));
			if (r < 0)
				r += (i + 1);
			perm[i] = source[r];
			perm2D[i] = (short)((perm[i] % 12) * 2);
			source[r] = source[i];
		}
	}
	
	/*
	 * Traditional evaluator
	 */
	
	// 2D SuperSimplex noise, standard point-evaluation
	// Lookup table implementation inspired by DigitalShadow
	public double eval(double x, double y) {
		double value = 0;
		
		//Get points for A2* lattice
		double s = 0.366025403784439 * (x + y);
		double xs = x + s, ys = y + s;
		
		//Get base points and offsets
		int xsb = fastFloor(xs), ysb = fastFloor(ys);
		double xsi = xs - xsb, ysi = ys - ysb;
		
		//Index to point list
		int a = (int)(xsi + ysi);
		int index =
			(a << 2) |
			(int)(xsi - ysi / 2 + 1 - a / 2.0) << 3 |
			(int)(ysi - xsi / 2 + 1 - a / 2.0) << 4;
		
		double ssi = (xsi + ysi) * -0.211324865405187;
		double xi = xsi + ssi, yi = ysi + ssi;

		//Point contributions
		for (int i = 0; i < 4; i++) {
			LatticePoint2D c = LOOKUP_2D[index + i];

			double dx = xi + c.dx, dy = yi + c.dy;
			double attn = 2.0 / 3.0 - dx * dx - dy * dy;
			if (attn <= 0) continue;

			int pxm = (xsb + c.xsv) & 1023, pym = (ysb + c.ysv) & 1023;
			int gi = perm2D[perm[pxm] ^ pym];
			double extrapolation = GRADIENTS_2D[gi] * dx
					+ GRADIENTS_2D[gi + 1] * dy;
			
			attn *= attn;
			value += attn * attn * extrapolation;
		}
		
		return value;
	}
	
	/*
	 * Generator
	 */ 
	
	public void generate(GenerateContext2D context, double[][] destination, int x0, int y0) {
		Queue<AreaGenLatticePoint2D> queue = new LinkedList<AreaGenLatticePoint2D>();
		Set<AreaGenLatticePoint2D> seen = new HashSet<AreaGenLatticePoint2D>();
		
		int height = destination.length;
		int width = destination[0].length;
		
		double frequencyScaling = context.frequencyScaling;
		double inverseScaling = context.inverseScaling;
		int scaledRadius = context.scaledRadius;
		double[][] kernel = context.kernel;
		
		// It seems that it's better for performance, to create a local copy.
		// - Slightly faster than generating the kernel here.
		// - Much faster than referencing it directly from the context object.
		// - Much faster than computing the kernel equation every time.
		// You can remove these lines if you find it's the opposite for you.
		kernel = new double[scaledRadius * 2][scaledRadius * 2];
		for (int yy = 0; yy < scaledRadius * 2; yy++) {
			kernel[yy] = (double[]) context.kernel[yy].clone();
		}
		
		// Get started with one point/vertex.
		// For some lattices, you might need to try a handful of points in the cell,
		// or flip a couple of coordinates, to guarantee it or a neighbor contributes.
		// For An* lattices, the base coordinate seems fine.
		double s0 = 0.366025403784439 * (x0 + y0);
		double x0s = (x0 + s0) * frequencyScaling, y0s = (y0 + s0) * frequencyScaling;
		int x0sb = fastFloor(x0s), y0sb = fastFloor(y0s);
		AreaGenLatticePoint2D firstPoint = new AreaGenLatticePoint2D(x0sb, y0sb, inverseScaling);
		firstPoint.computeGradient(this);
		queue.add(firstPoint);
		seen.add(firstPoint);
		
		while (!queue.isEmpty()) {
			AreaGenLatticePoint2D point = queue.remove();
			int destPointX = point.destPointX;
			int destPointY = point.destPointY;
			
			// Add contribution to destination
			int yy0 = destPointY - scaledRadius; if (yy0 < y0) yy0 = y0;
			int yy1 = destPointY + scaledRadius; if (yy1 > y0 + height) yy1 = y0 + height;
			
			// For each row of the contribution circle,
			for (int yy = yy0; yy < yy1; yy++) {
			
				// Setup bounds so we only loop over what we need to
				int thisScaledRadius = context.kernelBounds[yy - destPointY + scaledRadius];
				int xx0 = destPointX - thisScaledRadius; if (xx0 < x0) xx0 = x0;
				int xx1 = destPointX + thisScaledRadius; if (xx1 > x0 + width) xx1 = x0 + width;
				
				// For each point on that row
				for (int xx = xx0; xx < xx1; xx++) {
					int dx = xx - destPointX;
					int dy = yy - destPointY;
					double attn = kernel[dy + scaledRadius][dx + scaledRadius];
						
					// gOff accounts for our choice to offset the pregenerated kernel by (0.5, 0.5) to avoid the zero center.
					// I found almost no difference in performance using gOff vs not (under 1ns diff per value on my system)
					double extrapolation = point.gx * dx + point.gy * dy + point.gOff;
					destination[yy - y0][xx - x0] += attn * extrapolation;
					
				}
			}
			
			// For each neighbor of the point
			for (int i = 0; i < NEIGHBOR_MAP_2D.length; i++) {
				AreaGenLatticePoint2D neighbor = new AreaGenLatticePoint2D(
						point.xsv + NEIGHBOR_MAP_2D[i][0], point.ysv + NEIGHBOR_MAP_2D[i][1], inverseScaling);
						
				// If it's in range of the buffer region and not seen before
				if (neighbor.destPointX + scaledRadius >= x0 && neighbor.destPointX - scaledRadius <= x0 + width - 1
						&& neighbor.destPointY + scaledRadius >= y0 && neighbor.destPointY - scaledRadius <= y0 + height - 1
						&& !seen.contains(neighbor)) {
					
					// Since we're actually going to use it, we need to compute its gradient.
					neighbor.computeGradient(this);
					
					// Add it to the queue so we can process it at some point
					queue.add(neighbor);
					
					// Add it to the set so we don't add it to the queue again
					seen.add(neighbor);
				}
			}
		}
	}
	
	/*
	 * Utility
	 */
	
	private static int fastFloor(double x) {
		int xi = (int)x;
		return x < xi ? xi - 1 : xi;
	}
	
	/*
	 * Definitions
	 */

	private static final LatticePoint2D[] LOOKUP_2D;
	static {
		LOOKUP_2D = new LatticePoint2D[8 * 4];
		
		for (int i = 0; i < 8; i++) {
			int i1, j1, i2, j2;
			if ((i & 1) == 0) {
				if ((i & 2) == 0) { i1 = -1; j1 = 0; } else { i1 = 1; j1 = 0; }
				if ((i & 4) == 0) { i2 = 0; j2 = -1; } else { i2 = 0; j2 = 1; }
			} else {
				if ((i & 2) != 0) { i1 = 2; j1 = 1; } else { i1 = 0; j1 = 1; }
				if ((i & 4) != 0) { i2 = 1; j2 = 2; } else { i2 = 1; j2 = 0; }
			}
			LOOKUP_2D[i * 4 + 0] = new LatticePoint2D(0, 0);
			LOOKUP_2D[i * 4 + 1] = new LatticePoint2D(1, 1);
			LOOKUP_2D[i * 4 + 2] = new LatticePoint2D(i1, j1);
			LOOKUP_2D[i * 4 + 3] = new LatticePoint2D(i2, j2);
		}
	}
	
	// Hexagon surrounding each vertex.
	private static final int[][] NEIGHBOR_MAP_2D = {
		{ 1, 0 }, { 1, 1 }, { 0, 1 }, { 0, -1 }, { -1, -1 }, { -1, 0 }
	};
	
	private static class LatticePoint2D {
		int xsv, ysv;
		double dx, dy;
		public LatticePoint2D(int xsv, int ysv) {
			this.xsv = xsv; this.ysv = ysv;
			double ssv = (xsv + ysv) * -0.211324865405187;
			this.dx = -xsv - ssv;
			this.dy = -ysv - ssv;
		}
	}
	
	private static class AreaGenLatticePoint2D {
		int xsv, ysv;
		int destPointX, destPointY;
		double gx, gy, gOff;
		public AreaGenLatticePoint2D(int xsv, int ysv, double inverseScaling) {
			this.xsv = xsv; this.ysv = ysv;
			double ssv = (xsv + ysv) * -0.211324865405187;
			//this.destPointX = (int)Math.round((xsv + ssv) * inverseScaling + .5);
			//this.destPointY = (int)Math.round((ysv + ssv) * inverseScaling + .5);
			this.destPointX = (int)Math.ceil((xsv + ssv) * inverseScaling);
			this.destPointY = (int)Math.ceil((ysv + ssv) * inverseScaling);
		}
		public void computeGradient(SuperSimplex2DAreaGen instance) {
			int pxm = xsv & 1023, pym = ysv & 1023;
			int gi = instance.perm2D[instance.perm[pxm] ^ pym];
			this.gx = GRADIENTS_2D[gi]; this.gy = GRADIENTS_2D[gi + 1];
			this.gOff = 0.5 * (this.gx + this.gy); //to correct for (0.5, 0.5)-offset kernel
		}
		public int hashCode() {
			return xsv * 7841 + ysv;
		}
		public boolean equals(Object obj) {
			if (!(obj instanceof AreaGenLatticePoint2D)) return false;
			AreaGenLatticePoint2D other = (AreaGenLatticePoint2D) obj;
			return (other.xsv == this.xsv && other.ysv == this.ysv);
		}
	}
	
	public static class GenerateContext2D {
		
		double frequencyScaling;
		double inverseScaling;
		int scaledRadius;
		double[][] kernel;
		int[] kernelBounds;
		
		public GenerateContext2D(double frequencyScaling) {
		
			// These will be used by every call to generate
			this.frequencyScaling = frequencyScaling;
			inverseScaling = 1.0 / frequencyScaling;
			double preciseScaledRadius = Math.sqrt(2.0 / 3.0) * inverseScaling;
			double preciseScaledSquaredRadius = (2.0 / 3.0) * inverseScaling * inverseScaling;
			scaledRadius = (int)Math.ceil(preciseScaledRadius + 0.25); //0.25 because we offset center by 0.5
		
			// So will these
			kernel = new double[scaledRadius * 2][scaledRadius * 2];
			kernelBounds = new int[scaledRadius * 2];
			for (int yy = 0; yy < scaledRadius * 2; yy++) {
				
				// Pregenerate boundary of circle
				kernelBounds[yy] = (int)Math.ceil(
						Math.sqrt(1.0 - (yy + 0.5 - scaledRadius) * (yy + 0.5 - scaledRadius)
						/ (scaledRadius * scaledRadius)) * scaledRadius);
				
				// Pregenerate attenuation function
				for (int xx = 0; xx < scaledRadius * 2; xx++) {
					double dx = xx + 0.5 - scaledRadius;
					double dy = yy + 0.5 - scaledRadius;
					double attn = preciseScaledSquaredRadius - dx * dx - dy * dy;
					if (attn > 0) {
						attn *= frequencyScaling * frequencyScaling;
						attn *= attn;
						kernel[yy][xx] = attn * attn * frequencyScaling;
					} else {
						kernel[yy][xx] = 0.0;
					}
				}
			}
		}
	}
	
	// 2D Gradients: Dodecagon
	private static final double[] GRADIENTS_2D = new double[] {
		                  0,  18.518518518518519,
		  9.259259259259260,  16.037507477489605,
		 16.037507477489605,   9.259259259259260,
		 18.518518518518519,                   0,
		 16.037507477489605,  -9.259259259259260,
		  9.259259259259260, -16.037507477489605,
		                  0, -18.518518518518519,
		 -9.259259259259260, -16.037507477489605,
		-16.037507477489605,  -9.259259259259260,
		-18.518518518518519,                   0,
		-16.037507477489605,   9.259259259259260,
		 -9.259259259259260,  16.037507477489605,
		                  0,  18.518518518518519
	};
}