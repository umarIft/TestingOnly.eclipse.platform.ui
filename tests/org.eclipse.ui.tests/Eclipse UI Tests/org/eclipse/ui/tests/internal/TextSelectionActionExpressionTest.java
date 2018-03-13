/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.tests.internal;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.SubContributionItem;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.tests.harness.util.ActionUtil;
import org.eclipse.ui.tests.harness.util.FileUtil;
import org.eclipse.ui.tests.harness.util.UITestCase;

/**
 * This class contains tests for text selection action enablement
 */
public class TextSelectionActionExpressionTest extends UITestCase {
    protected IWorkbenchWindow fWindow;

    protected IWorkbenchPage fPage;

    public TextSelectionActionExpressionTest(String testName) {
        super(testName);
    }

    @Override
	protected void doSetUp() throws Exception {
        super.doSetUp();
        fWindow = openTestWindow();
        fPage = fWindow.getActivePage();
    }

    public void testAnyTextAction() throws Throwable {
        // Setup.
        ExtendedTextEditor editor = showTextEditor("anyText.exttxt");
        MenuManager mgr = getActionMenuManager(editor);

        // Test empty selection.
        selectAndUpdateMenu(editor, "", mgr);
        testAction(mgr, "anyText", true);

        // Test full selection.
        selectAndUpdateMenu(editor, "bob", mgr);
        testAction(mgr, "anyText", true);

        // Activate another view.
        fPage.showView(IPageLayout.ID_BOOKMARKS);
        testAction(mgr, "anyText", false);

        // Activate editor.
        // Test old selection.
        fPage.activate(editor);
        testAction(mgr, "anyText", true);

        // Test empty selection.
        selectAndUpdateMenu(editor, "", mgr);
        testAction(mgr, "anyText", true);
    }

    public void testEmptyTextAction() throws Throwable {
        // Setup.
        ExtendedTextEditor editor = showTextEditor("emptyText.exttxt");
        MenuManager mgr = getActionMenuManager(editor);

        // Test empty selection.
        selectAndUpdateMenu(editor, "", mgr);
        testAction(mgr, "emptyText", true);

        // Test full selection.
        selectAndUpdateMenu(editor, "bob", mgr);
        testAction(mgr, "emptyText", false);

        // Activate another view.
        fPage.showView(IPageLayout.ID_BOOKMARKS);
        testAction(mgr, "emptyText", false);

        // Activate editor.
        // Test old selection.
        fPage.activate(editor);
        testAction(mgr, "emptyText", false);

        // Test empty selection.
        selectAndUpdateMenu(editor, "", mgr);
        testAction(mgr, "emptyText", true);
    }

    public void testHelloTextAction() throws Throwable {
        // Setup.
        ExtendedTextEditor editor = showTextEditor("helloText.exttxt");
        MenuManager mgr = getActionMenuManager(editor);

        // Test empty selection.
        selectAndUpdateMenu(editor, "", mgr);
        testAction(mgr, "helloText", false);

        // Test wrong selection.
        selectAndUpdateMenu(editor, "bob", mgr);
        testAction(mgr, "helloText", false);

        // Test right selection.
        selectAndUpdateMenu(editor, "Hello", mgr);
        testAction(mgr, "helloText", true);

        // Activate another view.
        fPage.showView(IPageLayout.ID_BOOKMARKS);
        testAction(mgr, "helloText", false);

        // Activate editor.
        // Test old selection.
        fPage.activate(editor);
        testAction(mgr, "helloText", true);

        // Test wrong selection.
        selectAndUpdateMenu(editor, "bob", mgr);
        testAction(mgr, "helloText", false);
    }

    /**
     * Creates the list view.
     */
    private ExtendedTextEditor showTextEditor(String fileName) throws Throwable {
        IProject proj = FileUtil
                .createProject("TextSelectionActionExpressionTest");
        IFile file = FileUtil.createFile(fileName, proj);
        return (ExtendedTextEditor) IDE.openEditor(fPage, file, true);
    }

    /**
     * Select an object and fire about to show.
     */
    private void selectAndUpdateMenu(ExtendedTextEditor editor, String str,
            MenuManager mgr) throws Throwable {
        editor.setText(str);
        fPage.saveEditor(editor, false);
        ActionUtil.fireAboutToShow(mgr);
    }

    /**
     * Returns the menu manager containing the actions.
     */
    private MenuManager getActionMenuManager(ExtendedTextEditor editor)
            throws Throwable {
        fPage
                .showActionSet("org.eclipse.ui.tests.internal.TextSelectionActions");
        WorkbenchWindow win = (WorkbenchWindow) fWindow;
        IContributionItem item = win.getMenuBarManager().find(
                "org.eclipse.ui.tests.internal.TextSelectionMenu");
        while (item instanceof SubContributionItem) {
            item = ((SubContributionItem) item).getInnerItem();
            if (item instanceof MenuManager) {
				return (MenuManager) item;
			}
        }
        fail("Unable to find menu manager");
        return null;
    }

    /**
     * Tests the enablement / visibility of an action.
     */
    private void testAction(MenuManager mgr, String action, boolean expected)
            throws Throwable {
        assertEquals(action, expected, ActionUtil.getActionWithLabel(mgr,
                action).isEnabled());
    }
}
