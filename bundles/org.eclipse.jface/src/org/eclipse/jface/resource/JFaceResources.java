/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jface.resource;

import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.internal.JFaceActivator;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;

/**
 * Utility methods to access JFace-specific resources.
 * <p>
 * All methods declared on this class are static. This class cannot be
 * instantiated.
 * </p>
 * <p>
 * The following global state is also maintained by this class:
 * <ul>
 * <li>a font registry</li>
 * <li>a color registry</li>
 * <li>an image registry</li>
 * <li>a resource bundle</li>
 * </ul>
 * </p>
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
public class JFaceResources {

	/**
	 * The path to the icons in the resources.
	 */
	private final static String ICONS_PATH = "$nl$/icons/full/";//$NON-NLS-1$

	/**
	 * Map of Display onto DeviceResourceManager. Holds all the resources for
	 * the associated display.
	 */
	private static final Map registries = new HashMap();

	/**
	 * The symbolic font name for the banner font (value
	 * <code>"org.eclipse.jface.bannerfont"</code>).
	 */
	public static final String BANNER_FONT = "org.eclipse.jface.bannerfont"; //$NON-NLS-1$

	/**
	 * The JFace resource bundle; eagerly initialized.
	 */
	private static final ResourceBundle bundle = ResourceBundle
			.getBundle("org.eclipse.jface.messages"); //$NON-NLS-1$

	/**
	 * The JFace color registry; <code>null</code> until lazily initialized or
	 * explicitly set.
	 */
	private static ColorRegistry colorRegistry;

	/**
	 * The symbolic font name for the standard font (value
	 * <code>"org.eclipse.jface.defaultfont"</code>).
	 */
	public static final String DEFAULT_FONT = "org.eclipse.jface.defaultfont"; //$NON-NLS-1$

	/**
	 * The symbolic font name for the dialog font (value
	 * <code>"org.eclipse.jface.dialogfont"</code>).
	 */
	public static final String DIALOG_FONT = "org.eclipse.jface.dialogfont"; //$NON-NLS-1$

	/**
	 * The JFace font registry; <code>null</code> until lazily initialized or
	 * explicitly set.
	 */
	private static FontRegistry fontRegistry = null;

	/**
	 * The symbolic font name for the header font (value
	 * <code>"org.eclipse.jface.headerfont"</code>).
	 */
	public static final String HEADER_FONT = "org.eclipse.jface.headerfont"; //$NON-NLS-1$

	/**
	 * The JFace image registry; <code>null</code> until lazily initialized.
	 */
	private static ImageRegistry imageRegistry = null;

	/**
	 * The symbolic font name for the text font (value
	 * <code>"org.eclipse.jface.textfont"</code>).
	 */
	public static final String TEXT_FONT = "org.eclipse.jface.textfont"; //$NON-NLS-1$

	/**
	 * The symbolic font name for the viewer font (value
	 * <code>"org.eclipse.jface.viewerfont"</code>).
	 * 
	 * @deprecated This font is not in use
	 */
	public static final String VIEWER_FONT = "org.eclipse.jface.viewerfont"; //$NON-NLS-1$

	/**
	 * The symbolic font name for the window font (value
	 * <code>"org.eclipse.jface.windowfont"</code>).
	 * 
	 * @deprecated This font is not in use
	 */
	public static final String WINDOW_FONT = "org.eclipse.jface.windowfont"; //$NON-NLS-1$

	/**
	 * Returns the formatted message for the given key in JFace's resource
	 * bundle.
	 * 
	 * @param key
	 *            the resource name
	 * @param args
	 *            the message arguments
	 * @return the string
	 */
	public static String format(String key, Object[] args) {
		return MessageFormat.format(getString(key), args);
	}

	/**
	 * Returns the JFace's banner font. Convenience method equivalent to
	 * 
	 * <pre>
	 * JFaceResources.getFontRegistry().get(JFaceResources.BANNER_FONT)
	 * </pre>
	 * 
	 * @return the font
	 */
	public static Font getBannerFont() {
		return getFontRegistry().get(BANNER_FONT);
	}

	/**
	 * Returns the resource bundle for JFace itself. The resouble bundle is
	 * obtained from
	 * <code>ResourceBundle.getBundle("org.eclipse.jface.jface_nls")</code>.
	 * <p>
	 * Note that several static convenience methods are also provided on this
	 * class for directly accessing resources in this bundle.
	 * </p>
	 * 
	 * @return the resource bundle
	 */
	public static ResourceBundle getBundle() {
		return bundle;
	}

	/**
	 * Returns the color registry for JFace itself.
	 * <p>
	 * 
	 * @return the <code>ColorRegistry</code>.
	 * @since 3.0
	 */
	public static ColorRegistry getColorRegistry() {
		if (colorRegistry == null) {
			colorRegistry = new ColorRegistry();
			initializeDefaultColors();
		}
		return colorRegistry;
	}

	/**
	 * Returns the global resource manager for the given display
	 * 
	 * @since 3.1
	 * 
	 * @param toQuery
	 *            display to query
	 * @return the global resource manager for the given display
	 */
	public static ResourceManager getResources(final Display toQuery) {
		ResourceManager reg = (ResourceManager) registries.get(toQuery);

		if (reg == null) {
			final DeviceResourceManager mgr = new DeviceResourceManager(toQuery);
			reg = mgr;
			registries.put(toQuery, reg);
			toQuery.disposeExec(new Runnable() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see java.lang.Runnable#run()
				 */
				public void run() {
					mgr.dispose();
					registries.remove(toQuery);
				}
			});
		}

		return reg;
	}

	/**
	 * Returns the ResourceManager for the current display. May only be called
	 * from a UI thread.
	 * 
	 * @since 3.1
	 * 
	 * @return the global ResourceManager for the current display
	 */
	public static ResourceManager getResources() {
		return getResources(Display.getCurrent());
	}

	/**
	 * Returns JFace's standard font. Convenience method equivalent to
	 * 
	 * <pre>
	 * JFaceResources.getFontRegistry().get(JFaceResources.DEFAULT_FONT)
	 * </pre>
	 * 
	 * @return the font
	 */
	public static Font getDefaultFont() {
		return getFontRegistry().defaultFont();
	}

	/**
	 * Returns the descriptor for JFace's standard font. Convenience method
	 * equivalent to
	 * 
	 * <pre>
	 * JFaceResources.getFontRegistry().getDescriptor(JFaceResources.DEFAULT_FONT)
	 * </pre>
	 * 
	 * @return the font
	 * @since 3.3
	 */
	public static FontDescriptor getDefaultFontDescriptor() {
		return getFontRegistry().defaultFontDescriptor();
	}

	/**
	 * Returns the JFace's dialog font. Convenience method equivalent to
	 * 
	 * <pre>
	 * JFaceResources.getFontRegistry().get(JFaceResources.DIALOG_FONT)
	 * </pre>
	 * 
	 * @return the font
	 */
	public static Font getDialogFont() {
		return getFontRegistry().get(DIALOG_FONT);
	}

	/**
	 * Returns the descriptor for JFace's dialog font. Convenience method
	 * equivalent to
	 * 
	 * <pre>
	 * JFaceResources.getFontRegistry().getDescriptor(JFaceResources.DIALOG_FONT)
	 * </pre>
	 * 
	 * @return the font
	 * @since 3.3
	 */
	public static FontDescriptor getDialogFontDescriptor() {
		return getFontRegistry().getDescriptor(DIALOG_FONT);
	}

	/**
	 * Returns the font in JFace's font registry with the given symbolic font
	 * name. Convenience method equivalent to
	 * 
	 * <pre>
	 * JFaceResources.getFontRegistry().get(symbolicName)
	 * </pre>
	 * 
	 * If an error occurs, return the default font.
	 * 
	 * @param symbolicName
	 *            the symbolic font name
	 * @return the font
	 */
	public static Font getFont(String symbolicName) {
		return getFontRegistry().get(symbolicName);
	}

	/**
	 * Returns the font descriptor for in JFace's font registry with the given
	 * symbolic name. Convenience method equivalent to
	 * 
	 * <pre>
	 * JFaceResources.getFontRegistry().getDescriptor(symbolicName)
	 * </pre>
	 * 
	 * If an error occurs, return the default font.
	 * 
	 * @param symbolicName
	 *            the symbolic font name
	 * @return the font descriptor (never null)
	 * @since 3.3
	 */
	public static FontDescriptor getFontDescriptor(String symbolicName) {
		return getFontRegistry().getDescriptor(symbolicName);
	}

	/**
	 * Returns the font registry for JFace itself. If the value has not been
	 * established by an earlier call to <code>setFontRegistry</code>, is it
	 * initialized to
	 * <code>new FontRegistry("org.eclipse.jface.resource.jfacefonts")</code>.
	 * <p>
	 * Note that several static convenience methods are also provided on this
	 * class for directly accessing JFace's standard fonts.
	 * </p>
	 * 
	 * @return the JFace font registry
	 */
	public static FontRegistry getFontRegistry() {
		if (fontRegistry == null) {
			fontRegistry = new FontRegistry(
					"org.eclipse.jface.resource.jfacefonts"); //$NON-NLS-1$
		}
		return fontRegistry;
	}

	/**
	 * Returns the JFace's header font. Convenience method equivalent to
	 * 
	 * <pre>
	 * JFaceResources.getFontRegistry().get(JFaceResources.HEADER_FONT)
	 * </pre>
	 * 
	 * @return the font
	 */
	public static Font getHeaderFont() {
		return getFontRegistry().get(HEADER_FONT);
	}

	/**
	 * Returns the descriptor for JFace's header font. Convenience method
	 * equivalent to
	 * 
	 * <pre>
	 * JFaceResources.getFontRegistry().get(JFaceResources.HEADER_FONT)
	 * </pre>
	 * 
	 * @return the font descriptor (never null)
	 * @since 3.3
	 */
	public static FontDescriptor getHeaderFontDescriptor() {
		return getFontRegistry().getDescriptor(HEADER_FONT);
	}

	/**
	 * Returns the image in JFace's image registry with the given key, or
	 * <code>null</code> if none. Convenience method equivalent to
	 * 
	 * <pre>
	 * JFaceResources.getImageRegistry().get(key)
	 * </pre>
	 * 
	 * @param key
	 *            the key
	 * @return the image, or <code>null</code> if none
	 */
	public static Image getImage(String key) {
		return getImageRegistry().get(key);
	}

	/**
	 * Returns the image registry for JFace itself.
	 * <p>
	 * Note that the static convenience method <code>getImage</code> is also
	 * provided on this class.
	 * </p>
	 * 
	 * @return the JFace image registry
	 */
	public static ImageRegistry getImageRegistry() {
		if (imageRegistry == null) {
			imageRegistry = new ImageRegistry(
					getResources(Display.getCurrent()));
			initializeDefaultImages();
		}
		return imageRegistry;
	}

	/**
	 * Initialize default images in JFace's image registry.
	 * 
	 */
	private static void initializeDefaultImages() {

		Object bundle = null;
		try {
			bundle = JFaceActivator.getBundle();
		} catch (NoClassDefFoundError exception) {
			// Test to see if OSGI is present
		}
		declareImage(bundle, Wizard.DEFAULT_IMAGE, ICONS_PATH + "page.gif", //$NON-NLS-1$
				Wizard.class, "images/page.gif"); //$NON-NLS-1$

		// register default images for dialogs
		declareImage(bundle, Dialog.DLG_IMG_MESSAGE_INFO, ICONS_PATH
				+ "message_info.gif", Dialog.class, "images/message_info.gif"); //$NON-NLS-1$ //$NON-NLS-2$
		declareImage(bundle, Dialog.DLG_IMG_MESSAGE_WARNING, ICONS_PATH
				+ "message_warning.gif", Dialog.class, //$NON-NLS-1$
				"images/message_warning.gif"); //$NON-NLS-1$
		declareImage(bundle, Dialog.DLG_IMG_MESSAGE_ERROR, ICONS_PATH
				+ "message_error.gif", Dialog.class, "images/message_error.gif");//$NON-NLS-1$ //$NON-NLS-2$
		declareImage(bundle, Dialog.DLG_IMG_HELP,
				ICONS_PATH + "help.gif", Dialog.class, "images/help.gif");//$NON-NLS-1$ //$NON-NLS-2$
		declareImage(
				bundle,
				TitleAreaDialog.DLG_IMG_TITLE_BANNER,
				ICONS_PATH + "title_banner.png", TitleAreaDialog.class, "images/title_banner.gif");//$NON-NLS-1$ //$NON-NLS-2$
		declareImage(
				bundle,
				PreferenceDialog.PREF_DLG_TITLE_IMG,
				ICONS_PATH + "pref_dialog_title.gif", PreferenceDialog.class, "images/pref_dialog_title.gif");//$NON-NLS-1$ //$NON-NLS-2$
		declareImage(bundle, PopupDialog.POPUP_IMG_MENU, ICONS_PATH
				+ "popup_menu.gif", PopupDialog.class, "images/popup_menu.gif");//$NON-NLS-1$ //$NON-NLS-2$
		declareImage(
				bundle,
				PopupDialog.POPUP_IMG_MENU_DISABLED,
				ICONS_PATH + "popup_menu_disabled.gif", PopupDialog.class, "images/popup_menu_disabled.gif");//$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Declares a JFace image given the path of the image file (relative to the
	 * JFace plug-in). This is a helper method that creates the image descriptor
	 * and passes it to the main <code>declareImage</code> method.
	 * 
	 * @param bundle
	 *            the {@link Bundle} or <code>null</code> of the Bundle cannot
	 *            be found
	 * @param key
	 *            the symbolic name of the image
	 * @param path
	 *            the path of the image file relative to the base of the
	 *            workbench plug-ins install directory
	 * @param fallback
	 *            the {@link Class} where the fallback implementation of the
	 *            image is relative to
	 * @param fallbackPath
	 *            the path relative to the fallback {@link Class}
	 * 
	 */
	private static final void declareImage(Object bundle, String key,
			String path, Class fallback, String fallbackPath) {

		ImageDescriptor descriptor = null;

		if (bundle != null) {
			URL url = FileLocator.find((Bundle) bundle, new Path(path), null);
			if (url != null)
				descriptor = ImageDescriptor.createFromURL(url);
		}

		// If we failed then load from the backup file
		if (descriptor == null)
			descriptor = ImageDescriptor.createFromFile(fallback, fallbackPath);

		imageRegistry.put(key, descriptor);
	}

	/**
	 * Returns the resource object with the given key in JFace's resource
	 * bundle. If there isn't any value under the given key, the key is
	 * returned.
	 * 
	 * @param key
	 *            the resource name
	 * @return the string
	 */
	public static String getString(String key) {
		try {
			return bundle.getString(key);
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * Returns a list of string values corresponding to the given list of keys.
	 * The lookup is done with <code>getString</code>. The values are in the
	 * same order as the keys.
	 * 
	 * @param keys
	 *            a list of keys
	 * @return a list of corresponding string values
	 */
	public static String[] getStrings(String[] keys) {
		Assert.isNotNull(keys);
		int length = keys.length;
		String[] result = new String[length];
		for (int i = 0; i < length; i++) {
			result[i] = getString(keys[i]);
		}
		return result;
	}

	/**
	 * Returns JFace's text font. Convenience method equivalent to
	 * 
	 * <pre>
	 * JFaceResources.getFontRegistry().get(JFaceResources.TEXT_FONT)
	 * </pre>
	 * 
	 * @return the font
	 */
	public static Font getTextFont() {
		return getFontRegistry().get(TEXT_FONT);
	}

	/**
	 * Returns the descriptor for JFace's text font. Convenience method
	 * equivalent to
	 * 
	 * <pre>
	 * JFaceResources.getFontRegistry().getDescriptor(JFaceResources.TEXT_FONT)
	 * </pre>
	 * 
	 * @return the font descriptor (never null)
	 * @since 3.3
	 */
	public static FontDescriptor getTextFontDescriptor() {
		return getFontRegistry().getDescriptor(TEXT_FONT);
	}

	/**
	 * Returns JFace's viewer font. Convenience method equivalent to
	 * 
	 * <pre>
	 * JFaceResources.getFontRegistry().get(JFaceResources.VIEWER_FONT)
	 * </pre>
	 * 
	 * @return the font
	 * @deprecated This font is not in use
	 */
	public static Font getViewerFont() {
		return getFontRegistry().get(VIEWER_FONT);
	}

	/**
	 * Sets JFace's font registry to the given value. This method may only be
	 * called once; the call must occur before
	 * <code>JFaceResources.getFontRegistry</code> is invoked (either directly
	 * or indirectly).
	 * 
	 * @param registry
	 *            a font registry
	 */
	public static void setFontRegistry(FontRegistry registry) {
		Assert.isTrue(fontRegistry == null,
				"Font registry can only be set once."); //$NON-NLS-1$
		fontRegistry = registry;
	}

	/*
	 * (non-Javadoc) Declare a private constructor to block instantiation.
	 */
	private JFaceResources() {
		// no-op
	}

	/*
	 * Initialize any JFace colors that may not be initialized via a client.
	 */
	private static void initializeDefaultColors() {
		// JFace Colors that may not be defined in a workbench theme should be
		// defined here.
	}
}
