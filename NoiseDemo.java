/*
 * SuperSimplex Noise 2D Area Generation demo.
 */

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import javax.swing.*;

public class NoiseDemo
{
	private static final int WIDTH = 512;
	private static final int HEIGHT = 512;
	private static final double PERIOD = 64.0;
	private static final int OFF_X = 2048;
	private static final int OFF_Y = 2048;
	
	private static final boolean DEMONSTRATE_SUBTLE_DIFFERENCE = false;

	public static void main(String[] args)
			throws IOException {
		
		// Initialize
		OpenSimplex2S noise = new OpenSimplex2S(0);
		OpenSimplex2S.GenerateContext2D noiseBulk = new OpenSimplex2S.GenerateContext2D(OpenSimplex2S.LatticeOrientation2D.Standard, 1.0 / PERIOD, 1.0 / PERIOD, 1.0);
		
		// Generate
		double[][] buffer = new double[HEIGHT][WIDTH];
		noise.generate2(noiseBulk, buffer, OFF_X, OFF_Y);
		
		// Image
		BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < HEIGHT; y++)
		{
			for (int x = 0; x < WIDTH; x++)
			{
				double value = buffer[y][x]; if (value < -1) value = -1; if (value > 1) value = 1;
				if (DEMONSTRATE_SUBTLE_DIFFERENCE) {
					double oldValue = noise.noise2((x + OFF_X) * 1.0 / PERIOD, (y + OFF_Y) * 1.0 / PERIOD);
					value -= oldValue;
				}
				int rgb = 0x010101 * (int)((value + 1) * 127.5);
				image.setRGB(x, y, rgb);
			}
		}
		
		// Save it or show it
		if (args.length > 0 && args[0] != null) {
			ImageIO.write(image, "png", new File(args[0]));
			System.out.println("Saved image as " + args[0]);
		} else {
			JFrame frame = new JFrame();
			JLabel imageLabel = new JLabel();
			imageLabel.setIcon(new ImageIcon(image));
			frame.add(imageLabel);
			frame.pack();
			frame.setResizable(false);
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			frame.setVisible(true);
		}
		
	}
}