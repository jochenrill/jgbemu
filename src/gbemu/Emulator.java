package gbemu;

import java.io.IOException;

import javax.swing.JFrame;

import gbemu.cpu.CPU;
import gbemu.graphics.Display;
import gbemu.graphics.GPU;
import gbemu.memory.Cart;
import gbemu.memory.Memory;

public class Emulator {
	private CPU cpu;
	private Display display;
	private Memory memory;
	private GPU gpu;

	public Emulator() {
		this.display = new Display();
		this.gpu = new GPU(display);
		this.memory = new Memory(gpu);
		this.cpu = new CPU(memory, gpu);
	}

	public void start() {
		JFrame window = new JFrame();
		window.add(display);
		window.setSize(160, 144);
		window.setDefaultCloseOperation(3);

		try {
			memory.loadCart(new Cart("tetris.gb"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		window.setVisible(true);

		cpu.run();
	}
}
