package gbemu.memory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Cart {
	private short[] content;

	public Cart(String fileName) throws IOException {
		File cart = new File(fileName);
		FileInputStream s = new FileInputStream(cart);
		content = new short[(int) cart.length()];
		int pos = 0;
		int data;
		while ((data = s.read()) != -1) {
			content[pos++] = (short) (data & 0xFF);
		}
		s.close();
	}

	public short[] getBank(int index) {
		short[] result = new short[0x4000];
		if (index == 0) {
			System.arraycopy(content, 0x0, result, 0x0, 0x3FFF);
		} else if (index == 1) {
			System.arraycopy(content, 0x4000, result, 0x0, 0x3FFF);
		}
		return result;
	}
}
