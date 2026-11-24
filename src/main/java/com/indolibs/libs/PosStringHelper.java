package com.indolibs.libs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PosStringHelper {
    public static final int DEFAULT_WIDTH_58MM = 32;
    public static final int DEFAULT_WIDTH_80MM = 48;
    public static final Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1;

    private static final Locale INDONESIAN = new Locale("id", "ID");
    private static final ExecutorService PRINT_EXECUTOR = Executors.newSingleThreadExecutor();

    private final int width;
    private final Charset printerCharset;

    public interface PrintCallback {
        void onSuccess();
        void onError(Exception error);
    }

    public PosStringHelper() {
        this(DEFAULT_WIDTH_58MM, DEFAULT_CHARSET);
    }

    public PosStringHelper(int width) {
        this(width, DEFAULT_CHARSET);
    }

    public PosStringHelper(int width, Charset printerCharset) {
        this.width = width > 0 ? width : DEFAULT_WIDTH_58MM;
        this.printerCharset = printerCharset != null ? printerCharset : DEFAULT_CHARSET;
    }

    public static PosStringHelper thermal58mm() {
        return new PosStringHelper(DEFAULT_WIDTH_58MM, DEFAULT_CHARSET);
    }

    public static PosStringHelper thermal80mm() {
        return new PosStringHelper(DEFAULT_WIDTH_80MM, DEFAULT_CHARSET);
    }

    public int getWidth() {
        return width;
    }

    public Charset getPrinterCharset() {
        return printerCharset;
    }

    private static String repeatChar(char ch, int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }

    //Format Rupiah
    public static String currency(double amount) {
        long rounded = Math.round(amount);
        NumberFormat nf = NumberFormat.getNumberInstance(INDONESIAN);
        nf.setGroupingUsed(true);
        if (rounded < 0) {
            return "-Rp " + nf.format(Math.abs(rounded));
        }
        return "Rp " + nf.format(rounded);
    }

    public String receiptLine(String left, String right) {
        return receiptLine(left, right, this.width);
    }

    public String receiptLine(String left, String right, int lineWidth) {
        if (lineWidth <= 0) {
            return "";
        }
        String safeLeft = left == null ? "" : left.trim();
        String safeRight = right == null ? "" : right.trim();

        if (safeRight.length() >= lineWidth) {
            return safeRight.substring(0, lineWidth);
        }

        int maxLeft = lineWidth - safeRight.length() - 1;
        if (maxLeft < 0) {
            maxLeft = 0;
        }
        if (safeLeft.length() > maxLeft) {
            safeLeft = safeLeft.substring(0, maxLeft);
        }

        int spaces = lineWidth - safeLeft.length() - safeRight.length();
        if (spaces < 1) {
            spaces = 1;
        }

        return safeLeft + repeatChar(' ', spaces) + safeRight;
    }
	
	public String spaceRight(String text) {
		return spaceRight(text, this.width);
	}

	public String spaceRight(String text, int targetWidth) {
		if (targetWidth <= 0) {
			return "";
		}
		String safeText = text == null ? "" : text;
		if (safeText.length() >= targetWidth) {
			return safeText.substring(0, targetWidth);
		}
		return safeText + repeatChar(' ', targetWidth - safeText.length());
	}

	public String spaceLeft(String text) {
		return spaceLeft(text, this.width);
	}

	public String spaceLeft(String text, int targetWidth) {
		if (targetWidth <= 0) {
			return "";
		}
		String safeText = text == null ? "" : text;
		if (safeText.length() >= targetWidth) {
			return safeText.substring(0, targetWidth);
		}
		return repeatChar(' ', targetWidth - safeText.length()) + safeText;
	}

    public String center(String text) {
        return center(text, this.width);
    }

    public String center(String text, int targetWidth) {
        if (targetWidth <= 0) {
            return "";
        }
        String safeText = text == null ? "" : text.trim();
        if (safeText.length() >= targetWidth) {
            return safeText.substring(0, targetWidth);
        }
        int padding = (targetWidth - safeText.length()) / 2;
        return repeatChar(' ', padding) + safeText;
    }

    public String divider(char ch) {
        return divider(ch, this.width);
    }

    public String divider(char ch, int lineWidth) {
        if (lineWidth <= 0) {
            return "";
        }
        return repeatChar(ch, lineWidth);
    }

    public String padRight(String text, int targetWidth) {
        if (targetWidth <= 0) {
            return "";
        }
        String safeText = text == null ? "" : text;
        if (safeText.length() >= targetWidth) {
            return safeText.substring(0, targetWidth);
        }
        return safeText + repeatChar(' ', targetWidth -safeText.length());
    }

    public String padLeft(String text, int targetWidth) {
        if (targetWidth <= 0) {
            return "";
        }
        String safeText = text == null ? "" : text;
        if (safeText.length() >= targetWidth) {
            return safeText.substring(0, targetWidth);
        }
        return repeatChar(' ', targetWidth - safeText.length()) + safeText;
    }

    public List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.trim().isEmpty() || maxWidth <= 0) {
            return lines;
        }

        String[] words = text.trim().split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (word.length() > maxWidth) {
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                int idx = 0;
                while (idx < word.length()) {
                    int end = Math.min(idx + maxWidth, word.length());
                    lines.add(word.substring(idx, end));
                    idx = end;
                }
                continue;
            }

            if (current.length() == 0) {
                current.append(word);
            } else if (current.length() + 1 + word.length() <= maxWidth) {
                current.append(" ").append(word);
            } else {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }

        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    public String itemQtyPriceLine(int qty, double unitPrice, double subtotal) {
        String left = "  " + qty + "x @ " + currency(unitPrice);
        String right = currency(subtotal);
        return receiptLine(left, right, this.width);
    }
	
	public static String toPreviewString(byte[] rawEscPos, Charset charset) {
		if (rawEscPos == null || rawEscPos.length == 0) {
			return "";
		}
		Charset cs = (charset != null) ? charset : DEFAULT_CHARSET;
		ByteArrayOutputStream clean = new ByteArrayOutputStream(rawEscPos.length);

		int i = 0;
		while (i < rawEscPos.length) {
			int b = rawEscPos[i] & 0xFF;

			if (b == 0x1B && i + 1 < rawEscPos.length) { // ESC
				int cmd = rawEscPos[i + 1] & 0xFF;
				if (cmd == 0x40) { // ESC @ (initPrinter) -> 2 byte
					i += 2;
					continue;
				}
				if ((cmd == 0x45 || cmd == 0x2D || cmd == 0x4D || cmd == 0x64) && i + 2 < rawEscPos.length) {
					// ESC E n (bold), ESC - n (underline), ESC M n (italic/font), ESC d n (feedLines) -> 3 byte
					i += 3;
					continue;
				}
			}

			if (b == 0x1D && i + 3 < rawEscPos.length) { // GS
				int cmd = rawEscPos[i + 1] & 0xFF;
				if (cmd == 0x56) { // GS V m n (cutPaper) -> 4 byte
					i += 4;
					continue;
				}
			}

			// Buang control character lain kecuali LF (0x0A) dan CR (0x0D)
			if (b == 0x0A || b == 0x0D || (b >= 0x20 && b != 0x7F)) {
				clean.write(b);
			}
			i++;
		}

		return new String(clean.toByteArray(), cs);
	}
	
	public byte[] createByteArray(ByteArrayOutputStream out){
		if(out==null) return new  byte[]{};
		return out.toByteArray(); 
	}

    //Perintah ESC/POS Dasar
    public byte[] bold(boolean enable) {
        return new byte[] { 0x1B, 0x45, (byte) (enable ? 1 : 0) };
    }
	
	public void bold(ByteArrayOutputStream out)throws IOException {
        if (out != null)out.write(bold(true));
    }

    // mode: 0 = off, 1 = 1-dot (thin), 2 = 2-dot (thick)
    public byte[] underline(int mode) {
        int safeMode = (mode == 1 || mode == 2) ? mode : 0;
        return new byte[] { 0x1B, 0x2D, (byte) safeMode };
    }
	
    public byte[] underline(boolean enable) {
        return underline(enable ? 1 : 0);
    }
	
	public void underline(ByteArrayOutputStream out)throws IOException {
        if (out != null)out.write(underline(true));
    }

    public byte[] italic(boolean enable) {
        return new byte[] { 0x1B, 0x4D, (byte) (enable ? 1 : 0) };
    }
	
	public void italic(ByteArrayOutputStream out)throws IOException {
        if (out != null)out.write(italic(true));
    }

    public byte[] clearTextStyles() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(9);
        out.write(bold(false));
        out.write(underline(0));
        out.write(italic(false));
        return out.toByteArray();
    }
	
	public void clearTextStyles(ByteArrayOutputStream out)throws IOException {
		if (out != null)out.write(clearTextStyles());
    }

    public byte[] initPrinter() {
        return new byte[] { 0x1B, 0x40 };
    }
	
	public void initPrinter(ByteArrayOutputStream out)throws IOException {
		if (out != null)out.write(initPrinter());
    }

    public byte[] feedLines(int lines) {
        if (lines <= 0) {
            return new byte[0];
        }
        int safeLines = Math.min(lines, 255);
        return new byte[] { 0x1B, 0x64, (byte) safeLines };
    }
	
	public void feedLine(ByteArrayOutputStream out)throws IOException {
		if (out != null)out.write(feedLines(1));
    }

    public byte[] cutPaper() {
        return new byte[] { 0x1D, 0x56, 0x42, 0x00 };
    }
	
	public void cutPaper(ByteArrayOutputStream out)throws IOException {
		if (out != null)out.write(cutPaper());
    }

    public void writeLine(ByteArrayOutputStream out, String text) throws IOException {
        if (out == null) {
            return;
        }
        String safeText = text == null ? "" : text;
        out.write((safeText + "\n").getBytes(this.printerCharset));
    }

    public static void sendEscPosAsync(final OutputStream outputStream, final byte[] payload, final PrintCallback callback) {
        PRINT_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                if (outputStream == null) {
                    if (callback != null) {
                        callback.onError(new IllegalArgumentException("OutputStream printer tidak boleh null"));
                    }
                    return;
                }
                if (payload == null || payload.length == 0) {
                    if (callback != null) {
                        callback.onError(new IllegalArgumentException("Data struk kosong"));
                    }
                    return;
                }

                try {
                    outputStream.write(payload);
                    outputStream.flush();
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } catch (Exception e) {
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
            }
        });
    }
	
}