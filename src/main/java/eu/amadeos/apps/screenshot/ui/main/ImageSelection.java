package eu.amadeos.apps.screenshot.ui.main;

import java.awt.Image;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

/**
 * A picture on the clipboard. Java offers it to other programs as PNG (and other image types); when another
 * program puts something on the clipboard, {@code lost} runs.
 */
public record ImageSelection(Image image, Runnable lost) implements Transferable, ClipboardOwner {
    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DataFlavor.imageFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return DataFlavor.imageFlavor.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
        return image;
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        lost.run();
    }

    public static void put(Clipboard clipboard, Image image, Runnable lost) {
        ImageSelection selection = new ImageSelection(image, lost);
        clipboard.setContents(selection, selection);
    }
}
