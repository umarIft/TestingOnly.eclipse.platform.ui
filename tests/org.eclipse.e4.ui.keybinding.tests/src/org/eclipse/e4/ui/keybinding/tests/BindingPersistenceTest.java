/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.ui.keybinding.tests;

import java.util.HashMap;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.e4.ui.bindings.EBindingService;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.Scheme;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeyBinding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.keys.BindingService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.tests.harness.util.UITestCase;

/**
 * Test cases covering the various interaction between bindings. Bindings that
 * have been removed. Bindings that have been added. Inheritance of various
 * properties.
 * 
 * @since 3.1
 */
public final class BindingPersistenceTest extends UITestCase {

	private final String EMACS_SCHEME_ID = "org.eclipse.ui.emacsAcceleratorConfiguration";

	/**
	 * Constructor for <code>BindingPersistenceTest</code>.
	 * 
	 * @param name
	 *            The name of the test
	 */
	public BindingPersistenceTest(final String name) {
		super(name);
	}

	/**
	 * <p>
	 * Tests whether the preference store will be read automatically when a
	 * change to the preference store is made.
	 * </p>
	 * 
	 * @throws ParseException
	 *             If "ALT+SHIFT+Q A" cannot be parsed by KeySequence.
	 */
	public final void testAutoLoad() throws ParseException {
		// Get the services.
		ICommandService commandService = (ICommandService) fWorkbench
				.getAdapter(ICommandService.class);
		IBindingService bindingService = (IBindingService) fWorkbench
				.getAdapter(IBindingService.class);
		bindingService.readRegistryAndPreferences(commandService);

		// Check the pre-conditions.
		final String emacsSchemeId = EMACS_SCHEME_ID;
		assertFalse("The active scheme should be Emacs yet",
				emacsSchemeId.equals(bindingService.getActiveScheme().getId()));
		final KeySequence formalKeySequence = KeySequence
				.getInstance("ALT+SHIFT+Q A");
		final String commandId = "org.eclipse.ui.views.showView";
		Binding[] bindings = bindingService.getBindings();
		int i;
		for (i = 0; i < bindings.length; i++) {
			final Binding binding = bindings[i];
			if ((binding.getType() == Binding.USER)
					&& (formalKeySequence.equals(binding.getTriggerSequence()))) {
				final ParameterizedCommand command = binding
						.getParameterizedCommand();
				final String actualCommandId = (command == null) ? null
						: command.getCommand().getId();
				assertFalse("The command should not yet be bound",
						commandId.equals(actualCommandId));
				break;
			}
		}
		assertEquals("There shouldn't be a matching command yet",
				bindings.length, i);

		// Modify the preference store.
		final IPreferenceStore store = WorkbenchPlugin.getDefault()
				.getPreferenceStore();
		store.setValue(
				"org.eclipse.ui.commands",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><org.eclipse.ui.commands><activeKeyConfiguration keyConfigurationId=\""
						+ emacsSchemeId
						+ "\"/><keyBinding commandId=\""
						+ commandId
						+ "\" contextId=\"org.eclipse.ui.contexts.window\" keyConfigurationId=\"org.eclipse.ui.defaultAcceleratorConfiguration\" keySequence=\""
						+ formalKeySequence + "\"/></org.eclipse.ui.commands>");

		// Check that the values have changed.
		assertEquals("The active scheme should now be Emacs", emacsSchemeId,
				bindingService.getActiveScheme().getId());
		bindings = bindingService.getBindings();
		for (i = 0; i < bindings.length; i++) {
			final Binding binding = bindings[i];
			if ((binding.getType() == Binding.USER)
					&& (formalKeySequence.equals(binding.getTriggerSequence()))) {
				final ParameterizedCommand command = binding
						.getParameterizedCommand();
				final String actualCommandId = (command == null) ? null
						: command.getCommand().getId();
				assertEquals("The command should be bound to 'ALT+SHIFT+Q A'",
						commandId, actualCommandId);
				break;
			}
		}
		assertFalse("There should be a matching command now",
				(bindings.length == i));
	}

	public final void testSinglePlatform() throws Exception {
		// Get the services.
		ICommandService commandService = (ICommandService) fWorkbench
				.getAdapter(ICommandService.class);
		IBindingService bindingService = (IBindingService) fWorkbench
				.getAdapter(IBindingService.class);

		ParameterizedCommand about = new ParameterizedCommand(
				commandService.getCommand("org.eclipse.ui.help.aboutAction"),
				null);
		KeySequence m18A = KeySequence.getInstance("M1+8 A");
		KeySequence m18B = KeySequence.getInstance("M1+8 B");
		int numAboutBindings = 0;

		Binding[] bindings = bindingService.getBindings();
		for (int i = 0; i < bindings.length; i++) {
			final Binding binding = bindings[i];
			if (binding.getType() == Binding.SYSTEM) {
				String platform = binding.getPlatform();
				int idx = (platform == null ? -1 : platform.indexOf(','));
				assertEquals(binding.toString(), -1, idx);
				if (about.equals(binding.getParameterizedCommand())) {
					if (m18A.equals(binding.getTriggerSequence())) {
						numAboutBindings++;
						assertNull("M+8 A", binding.getPlatform());
					} else if (m18B.equals(binding.getTriggerSequence())) {
						numAboutBindings++;
						// assertEquals(Util.WS_CARBON, binding.getPlatform());
						// temp work around for honouring carbon bindings
						assertTrue(
								"failure for platform: "
										+ binding.getPlatform(),
								Util.WS_CARBON.equals(binding.getPlatform())
										|| Util.WS_COCOA.equals(binding
												.getPlatform()));
					}
				}
			}
		}
		if (Util.WS_CARBON.equals(SWT.getPlatform())
				|| Util.WS_COCOA.equals(SWT.getPlatform())) {
			assertEquals(2, numAboutBindings);
		} else {
			assertEquals(1, numAboutBindings);
		}
	}

	public final void testBindingTransform() throws Exception {
		ICommandService commandService = (ICommandService) fWorkbench
				.getAdapter(ICommandService.class);
		IBindingService bindingService = (IBindingService) fWorkbench
				.getAdapter(IBindingService.class);

		ParameterizedCommand addWS = new ParameterizedCommand(
				commandService
						.getCommand("org.eclipse.ui.navigate.addToWorkingSet"),
				null);
		KeySequence m18w = KeySequence.getInstance("M1+8 W");
		KeySequence m28w = KeySequence.getInstance("M2+8 W");
		boolean foundDeleteMarker = false;
		int numOfMarkers = 0;
		Binding[] bindings = bindingService.getBindings();
		for (int i = 0; i < bindings.length; i++) {
			final Binding binding = bindings[i];
			if (binding.getType() == Binding.SYSTEM) {
				String platform = binding.getPlatform();
				int idx = (platform == null ? -1 : platform.indexOf(','));
				assertEquals(binding.toString(), -1, idx);
				if (addWS.equals(binding.getParameterizedCommand())) {
					if (m18w.equals(binding.getTriggerSequence())) {
						numOfMarkers++;
						assertNull(m18w.format(), binding.getPlatform());
					} else if (m28w.equals(binding.getTriggerSequence())) {
						numOfMarkers++;
						assertTrue(platform, Util.WS_CARBON.equals(platform)
								|| Util.WS_COCOA.equals(platform)
								|| Util.WS_GTK.equals(platform)
								|| Util.WS_WIN32.equals(platform));
					}
				} else if (binding.getParameterizedCommand() == null
						&& m18w.equals(binding.getTriggerSequence())) {
					assertTrue(
							platform,
							Util.WS_CARBON.equals(platform)
									|| Util.WS_COCOA.equals(platform)
									|| Util.WS_GTK.equals(platform)
									|| Util.WS_WIN32.equals(platform));
					numOfMarkers++;
					foundDeleteMarker = true;
				}
			}
		}
		assertEquals(3, numOfMarkers);
		assertTrue("Unable to find delete marker", foundDeleteMarker);
		TriggerSequence[] activeBindingsFor = bindingService
				.getActiveBindingsFor(addWS);
		assertEquals(1, activeBindingsFor.length);
	}

	public void testModifierWithPlatform() throws Exception {

		ICommandService commandService = (ICommandService) fWorkbench
				.getAdapter(ICommandService.class);
		IBindingService bindingService = (IBindingService) fWorkbench
				.getAdapter(IBindingService.class);
		ParameterizedCommand importCmd = new ParameterizedCommand(
				commandService.getCommand("org.eclipse.ui.file.import"), null);
		Binding[] bindings = bindingService.getBindings();
		int numOfMarkers = 0;
		for (int i = 0; i < bindings.length; i++) {
			final Binding binding = bindings[i];
			if (binding.getType() != Binding.SYSTEM)
				continue;

			if (importCmd.equals(binding.getParameterizedCommand())) {
				// make sure the modifier is applied
				assertEquals(KeySequence.getInstance("M2+8 I"),
						binding.getTriggerSequence());
				numOfMarkers++;
			}
		}

		// only one binding, if the platform matches
		assertEquals(numOfMarkers, 1);
	}

	public void testModifierNotApplied() throws Exception {

		ICommandService commandService = (ICommandService) fWorkbench
				.getAdapter(ICommandService.class);
		IBindingService bindingService = (IBindingService) fWorkbench
				.getAdapter(IBindingService.class);
		ParameterizedCommand exportCmd = new ParameterizedCommand(
				commandService.getCommand("org.eclipse.ui.file.export"), null);
		Binding[] bindings = bindingService.getBindings();
		for (int i = 0; i < bindings.length; i++) {
			final Binding binding = bindings[i];
			if (binding.getType() != Binding.SYSTEM)
				continue;

			if (exportCmd.equals(binding.getParameterizedCommand())) {
				// make sure the modifier is NOT applied
				assertEquals(KeySequence.getInstance("M1+8 E"),
						binding.getTriggerSequence());
				break;
			}
		}
	}

	public void testDifferentPlatform() throws Exception {

		ICommandService commandService = (ICommandService) fWorkbench
				.getAdapter(ICommandService.class);
		IBindingService bindingService = (IBindingService) fWorkbench
				.getAdapter(IBindingService.class);
		ParameterizedCommand backCmd = new ParameterizedCommand(
				commandService.getCommand("org.eclipse.ui.navigate.back"), null);
		Binding[] bindings = bindingService.getBindings();
		for (int i = 0; i < bindings.length; i++) {
			final Binding binding = bindings[i];
			if (binding.getType() != Binding.SYSTEM)
				continue;

			if (backCmd.equals(binding.getParameterizedCommand())) {
				// make sure the modifier is NOT applied
				// this will fail on Photon (but Paul thinks we'll never run the
				// test suite on that platform :-)
				assertEquals(KeySequence.getInstance("M1+8 Q"),
						binding.getTriggerSequence());
				// and the platform should be null
				assertNull(binding.getPlatform());
				break;
			}
		}
	}

	public void testAboutBinding() throws Exception {
		ICommandService commandService = (ICommandService) fWorkbench
				.getAdapter(ICommandService.class);
		IBindingService bindingService = (IBindingService) fWorkbench
				.getAdapter(IBindingService.class);

		final Scheme activeScheme = bindingService.getActiveScheme();
		final Binding[] originalBindings = bindingService.getBindings();

		ParameterizedCommand aboutCmd = new ParameterizedCommand(
				commandService
						.getCommand(IWorkbenchCommandConstants.HELP_ABOUT),
				null);
		ParameterizedCommand activateEditorCmd = new ParameterizedCommand(
				commandService
						.getCommand(IWorkbenchCommandConstants.WINDOW_ACTIVATE_EDITOR),
				null);

		final KeySequence keyF12 = KeySequence.getInstance("F12");
		final KeySequence keyAltCtrlShiftI = KeySequence
				.getInstance("ALT+CTRL+SHIFT+I");
		final Binding editorBinding = bindingService.getPerfectMatch(keyF12);
		assertNotNull(editorBinding);
		assertEquals(activateEditorCmd, editorBinding.getParameterizedCommand());

		EBindingService ebs = (EBindingService) fWorkbench
				.getService(EBindingService.class);
		HashMap<String, String> attrs = new HashMap<String, String>();
		attrs.put(EBindingService.TYPE_ATTR_TAG, "user");
		final Binding localAboutBinding = ebs.createBinding(keyF12, aboutCmd,
				IContextService.CONTEXT_ID_WINDOW, attrs);
		assertEquals(Binding.USER, localAboutBinding.getType());

		// test unbinding a system binding and adding a user binding (same
		// triggers and context)
		final Binding[] bindings = originalBindings;
		Binding[] added = new Binding[bindings.length + 2];
		System.arraycopy(bindings, 0, added, 0, bindings.length);

		Binding del = new KeyBinding(keyF12, null,
				IBindingService.DEFAULT_DEFAULT_ACTIVE_SCHEME_ID,
				IContextService.CONTEXT_ID_WINDOW, null, null, null,
				Binding.USER);
		added[bindings.length] = del;
		added[bindings.length + 1] = localAboutBinding;
		bindingService.savePreferences(activeScheme, added);

		// the match should be the user binding that we just added
		final Binding secondMatch = bindingService.getPerfectMatch(keyF12);
		assertNotNull(secondMatch);
		assertEquals(aboutCmd, secondMatch.getParameterizedCommand());

		// go back to the defaults
		bindingService.savePreferences(activeScheme, originalBindings);
		final Binding thirdMatch = bindingService.getPerfectMatch(keyF12);
		assertNotNull(thirdMatch);
		assertEquals(activateEditorCmd, thirdMatch.getParameterizedCommand());

		// try assigning alt-ctrl-shift-i (no other binding uses this for this
		// context) to the 'about' command
		final Binding localAboutBinding1 = ebs.createBinding(keyAltCtrlShiftI,
				aboutCmd, IContextService.CONTEXT_ID_WINDOW, attrs);
		assertEquals(Binding.USER, localAboutBinding1.getType());
		Binding[] added1 = new Binding[bindings.length + 1];
		System.arraycopy(bindings, 0, added1, 0, bindings.length);
		added1[bindings.length] = localAboutBinding1;

		bindingService.savePreferences(activeScheme, added1);
		final Binding fourthMatch = bindingService
				.getPerfectMatch(keyAltCtrlShiftI);
		assertNotNull(fourthMatch);
		assertEquals(aboutCmd, fourthMatch.getParameterizedCommand());
	}

	public void testAboutBindingIn3x() throws Exception {
		ICommandService commandService = (ICommandService) fWorkbench
				.getAdapter(ICommandService.class);
		IBindingService bindingService = (IBindingService) fWorkbench
				.getAdapter(IBindingService.class);

		final Scheme activeScheme = bindingService.getActiveScheme();

		ParameterizedCommand aboutCmd = new ParameterizedCommand(
				commandService
						.getCommand(IWorkbenchCommandConstants.HELP_ABOUT),
				null);
		ParameterizedCommand activateEditorCmd = new ParameterizedCommand(
				commandService
						.getCommand(IWorkbenchCommandConstants.WINDOW_ACTIVATE_EDITOR),
				null);

		final KeySequence keyF12 = KeySequence.getInstance("F12");
		final Binding editorBinding = bindingService.getPerfectMatch(keyF12);
		assertNotNull(editorBinding);
		assertEquals(activateEditorCmd, editorBinding.getParameterizedCommand());

		EBindingService ebs = (EBindingService) fWorkbench
				.getService(EBindingService.class);
		HashMap<String, String> attrs = new HashMap<String, String>();
		attrs.put(EBindingService.TYPE_ATTR_TAG, "user");
		final Binding localAboutBinding = ebs.createBinding(keyF12, aboutCmd,
				IContextService.CONTEXT_ID_WINDOW, attrs);
		assertEquals(Binding.USER, localAboutBinding.getType());

		final Binding[] bindings = bindingService.getBindings();
		Binding[] added = new Binding[bindings.length + 1];
		System.arraycopy(bindings, 0, added, 0, bindings.length);

		added[bindings.length] = localAboutBinding;
		bindingService.savePreferences(activeScheme, added);

		final Binding secondMatch = bindingService.getPerfectMatch(keyF12);
		// fails
		assertNotNull(secondMatch);
		assertEquals(aboutCmd, secondMatch.getParameterizedCommand());
	}

	public void testAboutBindingEmacs() throws Exception {

		ICommandService commandService = (ICommandService) fWorkbench
				.getAdapter(ICommandService.class);
		IBindingService bindingService = (IBindingService) fWorkbench
				.getAdapter(IBindingService.class);

		final Scheme emacsScheme = bindingService.getScheme(EMACS_SCHEME_ID);
		assertNotNull(emacsScheme);
		final Binding[] originalBindings = bindingService.getBindings();
		bindingService.savePreferences(emacsScheme, originalBindings);

		ParameterizedCommand findAndReplaceCmd = new ParameterizedCommand(
				commandService
						.getCommand(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE),
				null);
		ParameterizedCommand aboutCmd = new ParameterizedCommand(
				commandService
						.getCommand(IWorkbenchCommandConstants.HELP_ABOUT),
				null);

		final KeySequence keyAltR = KeySequence.getInstance("ALT+R");
		final KeySequence keyAltCtrlShiftI = KeySequence
				.getInstance("ALT+CTRL+SHIFT+I");
		final Binding findAndReplaceBinding = bindingService
				.getPerfectMatch(keyAltR);

		assertNotNull(findAndReplaceBinding);
		assertEquals(findAndReplaceCmd,
				findAndReplaceBinding.getParameterizedCommand());
		assertEquals(EMACS_SCHEME_ID, findAndReplaceBinding.getSchemeId());

		EBindingService ebs = (EBindingService) fWorkbench
				.getService(EBindingService.class);
		HashMap<String, String> attrs = new HashMap<String, String>();
		attrs.put(EBindingService.TYPE_ATTR_TAG, "user");
		attrs.put(EBindingService.SCHEME_ID_ATTR_TAG, EMACS_SCHEME_ID);
		final Binding localAboutBinding = ebs.createBinding(keyAltR, aboutCmd,
				IContextService.CONTEXT_ID_WINDOW, attrs);
		assertNotNull(localAboutBinding);
		assertEquals(Binding.USER, localAboutBinding.getType());
		assertEquals(EMACS_SCHEME_ID, localAboutBinding.getSchemeId());

		final Binding[] bindings = originalBindings;
		Binding[] added = new Binding[bindings.length + 2];
		System.arraycopy(bindings, 0, added, 0, bindings.length);

		Binding del = new KeyBinding(keyAltR, null, EMACS_SCHEME_ID,
				IContextService.CONTEXT_ID_WINDOW, null, null, null,
				Binding.USER);
		added[bindings.length] = del;
		added[bindings.length + 1] = localAboutBinding;
		bindingService.savePreferences(emacsScheme, added);

		// the match should be the user binding that we just added
		final Binding secondMatch = bindingService.getPerfectMatch(keyAltR);
		assertNotNull(secondMatch);
		assertEquals(aboutCmd, secondMatch.getParameterizedCommand());

		// go back to the defaults
		bindingService.savePreferences(emacsScheme, originalBindings);
		final Binding thirdMatch = bindingService.getPerfectMatch(keyAltR);
		assertNotNull(thirdMatch);
		assertEquals(findAndReplaceCmd, thirdMatch.getParameterizedCommand());

		// try assigning alt-ctrl-shift-i (no other binding uses this for this
		// context) to the 'about' command
		final Binding localAboutBinding1 = ebs.createBinding(keyAltCtrlShiftI,
				aboutCmd, IContextService.CONTEXT_ID_WINDOW, attrs);
		assertNotNull(localAboutBinding1);
		assertEquals(Binding.USER, localAboutBinding1.getType());
		assertEquals(EMACS_SCHEME_ID, localAboutBinding.getSchemeId());

		Binding[] added1 = new Binding[bindings.length + 1];
		System.arraycopy(bindings, 0, added1, 0, bindings.length);
		added1[bindings.length] = localAboutBinding1;

		bindingService.savePreferences(emacsScheme, added1);
		final Binding fourthMatch = bindingService
				.getPerfectMatch(keyAltCtrlShiftI);
		assertNotNull(fourthMatch);
		assertEquals(aboutCmd, fourthMatch.getParameterizedCommand());
		assertEquals(EMACS_SCHEME_ID, fourthMatch.getSchemeId());
	}

	// the 'paste' key binding overrides the 'redo' key binding on Windows
	// platforms
	public void testPasteAndRedoBindingEmacs() throws Exception {
		ICommandService commandService = (ICommandService) fWorkbench
				.getAdapter(ICommandService.class);
		IBindingService bindingService = (IBindingService) fWorkbench
				.getAdapter(IBindingService.class);

		final Scheme emacsScheme = bindingService.getScheme(EMACS_SCHEME_ID);
		assertNotNull(emacsScheme);
		final Scheme defaultScheme = bindingService
				.getScheme(IBindingService.DEFAULT_DEFAULT_ACTIVE_SCHEME_ID);
		assertNotNull(defaultScheme);

		final Binding[] originalBindings = bindingService.getBindings();
		bindingService.savePreferences(emacsScheme, originalBindings);

		ParameterizedCommand pasteCmd = new ParameterizedCommand(
				commandService
						.getCommand(IWorkbenchCommandConstants.EDIT_PASTE),
				null);
		ParameterizedCommand redoCmd = new ParameterizedCommand(
				commandService.getCommand(IWorkbenchCommandConstants.EDIT_REDO),
				null);

		final KeySequence keyCtrlY = KeySequence.getInstance("CTRL+Y");

		final Binding pasteBinding = bindingService.getPerfectMatch(keyCtrlY);
		assertNotNull(pasteBinding);
		assertEquals(pasteCmd, pasteBinding.getParameterizedCommand());
		assertEquals(EMACS_SCHEME_ID, pasteBinding.getSchemeId());

		// reset the scheme
		bindingService.savePreferences(defaultScheme, originalBindings);
		final Binding redoBinding = bindingService.getPerfectMatch(keyCtrlY);
		assertNotNull(redoBinding);
		assertEquals(redoCmd, redoBinding.getParameterizedCommand());
		assertEquals(IBindingService.DEFAULT_DEFAULT_ACTIVE_SCHEME_ID,
				redoBinding.getSchemeId());
	}

	// the 'paste' key binding overrides the 'redo' key binding and can be
	// put back
	public void testPasteBindingEmacs() throws Exception {
		ICommandService commandService = (ICommandService) fWorkbench
				.getAdapter(ICommandService.class);
		IBindingService bindingService = (IBindingService) fWorkbench
				.getAdapter(IBindingService.class);

		final Scheme emacsScheme = bindingService.getScheme(EMACS_SCHEME_ID);
		assertNotNull(emacsScheme);
		final Binding[] originalBindings = bindingService.getBindings();
		bindingService.savePreferences(emacsScheme, originalBindings);

		ParameterizedCommand pasteCmd = new ParameterizedCommand(
				commandService
						.getCommand(IWorkbenchCommandConstants.EDIT_PASTE),
				null);

		final KeySequence keyCtrlY = KeySequence.getInstance("CTRL+Y");

		final Binding pasteBinding = bindingService.getPerfectMatch(keyCtrlY);
		assertNotNull(pasteBinding);
		assertEquals(pasteCmd, pasteBinding.getParameterizedCommand());
		assertEquals(EMACS_SCHEME_ID, pasteBinding.getSchemeId());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.tests.harness.util.UITestCase#doTearDown()
	 */
	protected void doTearDown() throws Exception {
		final IPreferenceStore store = WorkbenchPlugin.getDefault()
				.getPreferenceStore();
		store.setValue(
				"org.eclipse.ui.commands",
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><org.eclipse.ui.commands><activeKeyConfiguration keyConfigurationId=\""
						+ IBindingService.DEFAULT_DEFAULT_ACTIVE_SCHEME_ID
						+ "\"/></org.eclipse.ui.commands>");
		IBindingService bindingService = (IBindingService) fWorkbench
				.getAdapter(IBindingService.class);

		// reset keybindings
		bindingService.readRegistryAndPreferences(null);
		final Scheme activeScheme = bindingService
				.getScheme(IBindingService.DEFAULT_DEFAULT_ACTIVE_SCHEME_ID);
		final Binding[] originalBindings = bindingService.getBindings();
		bindingService.savePreferences(activeScheme, originalBindings);
		super.doTearDown();
	}
}
