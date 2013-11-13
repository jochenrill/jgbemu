package gbemu.graphics;

import java.awt.Color;

public class GPU {

	private int clock;
	private Display d;

	// Registers
	private int line;
	private int scrollX;
	private int scrollY;
	private byte tileMap;
	private byte tileSet;
	private byte bgOn;
	private byte spriteSize;
	private byte spritesOn;
	private byte windowOn;
	private byte windowMap;
	private byte displayOn;

	private short[] colors;

	private short[] vram;

	private enum MODES {
		SCANLINE1, SCANLINE2, HBLANK, VBLANK,
	};

	private MODES mode;

	public GPU(Display d) {
		this.d = d;
		this.mode = MODES.SCANLINE1;
		vram = new short[0x2000];
		colors = new short[4];
		colors[0] = 255;
		colors[1] = 192;
		colors[2] = 96;
		colors[3] = 0;

	}

	public void tick(int time) {
		clock += time;

		switch (mode) {
		case SCANLINE1:
			if (clock >= 80) {
				mode = MODES.SCANLINE2;
				clock = 0;
			}
			break;
		case SCANLINE2:
			if (clock >= 172) {
				mode = MODES.HBLANK;
				clock = 0;
			}
			drawLine();
			break;
		case HBLANK:
			if (clock >= 204) {
				clock = 0;
				line++;
				if (line == 143) {
					mode = MODES.VBLANK;
					pushImage();

				} else {
					mode = MODES.SCANLINE1;
				}
			}
			break;
		case VBLANK:
			if (clock >= 456) {
				clock = 0;
				line++;
				if (line > 153) {
					line = 0;
					mode = MODES.SCANLINE1;
				}
			}
			break;
		}

	}

	private void drawLine() {
		// base address of current map in vram
		int mapBaseAddress = (tileMap == 0) ? 0x1800 : 0x1c00;
		// base address of current tile set in vram
		int tileBaseAddress = (tileSet == 0) ? 0x0800 : 0x0;

		// number of line in which the tile is located in the map
		int verticalTileNumber = (scrollY + line) / 8;
		// number of tile in the line
		int horizontalTileNumber = scrollX / 8;

		// total address, 32 tiles per line
		int tileAddress = verticalTileNumber * 32 + horizontalTileNumber
				+ mapBaseAddress;

		// tile number. if signed value, add 256 to get an address
		int startTileNumber = 0;
		if (tileSet == 0) {
			startTileNumber = ((byte) vram[tileAddress]) + 128;
		} else {
			startTileNumber = vram[tileAddress];
		}

		// x,y value in current tile
		int y = (scrollY + line) % 8;
		int x = scrollX % 8;

		for (int i = 0; i < 160; i++) {

			int line1 = vram[tileBaseAddress + startTileNumber * 16 + y * 2];
			int line2 = vram[tileBaseAddress + startTileNumber * 16 + y * 2 + 1];

			int pixel = (((line2 & (0x80 >>> x)) >>> (7 - x - 1)) | ((line1 & (0x80 >>> x)) >>> (7 - x)));
			d.getFrameBuffer().setRGB(
					i,
					line,
					new Color(colors[pixel], colors[pixel], colors[pixel], 255)
							.getRGB());

			x++;
			if (x == 8) {
				// next tile
				x = 0;
				horizontalTileNumber = (horizontalTileNumber + 1) % 32;
				tileAddress = verticalTileNumber * 32 + horizontalTileNumber
						+ mapBaseAddress;
				if (tileSet == 0) {
					startTileNumber = ((byte) vram[tileAddress]) + 128;
				} else {
					startTileNumber = vram[tileAddress];
				}

			}

		}

	}

	private void pushImage() {
		d.repaint();
	}

	public int readByte(int addr) {
		if (addr >= 0xFF40) {
			// I/O
			if (addr == 0xFF40) {
				return displayOn << 7 | windowMap << 6 | windowOn << 5
						| tileSet << 4 | tileMap << 3 | spriteSize << 2
						| spritesOn << 1 | bgOn;
			} else if (addr == 0xFF42) {
				return scrollY;
			} else if (addr == 0xFF43) {
				return scrollX;
			} else if (addr == 0xFF44) {
				return line;
			} else if (addr == 0xFF47) {
				int result = 0;
				for (int i = 0; i < 4; i++) {
					switch (colors[i]) {
					case 255:
						result = result | 0x03;
						break;
					case 192:
						result = result | 0x02;
						break;
					case 92:
						result = result | 0x01;
						break;
					case 0:
						break;
					}
					result = result << 2;
				}
				return result;
			} else {
				return 0;
			}
		} else {
			return vram[addr - 0x8000];
		}
	}

	public void writeByte(int addr, int value) {
		if (addr >= 0xFF40) {
			// I/O
			if (addr == 0xFF40) {
				tileMap = (byte) ((value & 0x08) >>> 3);
				tileSet = (byte) ((value & 0x10) >>> 4);
				bgOn = (byte) (value & 0x01);
				spritesOn = (byte) ((value & 0x02) >>> 1);
				spriteSize = (byte) ((value & 0x04) >>> 2);
				windowOn = (byte) ((value & 0x20) >>> 5);
				windowMap = (byte) ((value & 0x40) >>> 6);
				displayOn = (byte) ((value & 0x80) >>> 7);
			} else if (addr == 0xFF42) {
				scrollY = value;
			} else if (addr == 0xFF43) {
				scrollX = value;
			} else if (addr == 0xFF44) {
				line = value;
			} else if (addr == 0xFF47) {
				for (int i = 0; i < 4; i++) {
					switch ((value >>> i * 2) & 0x03) {
					case 3:
						colors[i] = 255;
						break;
					case 2:
						colors[i] = 192;
						break;
					case 1:
						colors[i] = 96;
						break;
					case 0:
						colors[i] = 0;
						break;
					}
				}
			}
		} else {
			vram[addr - 0x8000] = (short) value;
		}
	}
}
