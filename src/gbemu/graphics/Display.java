package gbemu.graphics;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class Display extends JPanel {

	private BufferedImage fb = new BufferedImage(160, 144,
			BufferedImage.TYPE_4BYTE_ABGR);

	public Display() {
		Graphics g = fb.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, fb.getWidth(), fb.getHeight());
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.drawImage(fb, 0, 0, null);
	}

	public BufferedImage getFrameBuffer() {
		return fb;
	}

}
