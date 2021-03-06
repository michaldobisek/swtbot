/*******************************************************************************
 * Copyright (c) 2009 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/
package org.eclipse.swtbot.eclipse.dsl;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.swt.widgets.Widget;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEditor;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.Result;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.utils.StringConverter;
import org.eclipse.swtbot.swt.finder.utils.StringUtils;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * A default implementation of the workbench. May be subclassed to provide alternate implementations for different
 * versions of Eclipse.
 * <p>
 * Do not access directly. Instead use <code>Eclipse.workbench()</code>
 * </p>
 * 
 * @author David Green
 * @see Eclipse
 */
public class DefaultWorkbench extends Workbench {

	/** The bot that may be used to drive the workbench. */
	protected SWTWorkbenchBot	bot;

	/**
	 * Creates an instance of the default workbench.
	 * 
	 * @param bot the bot that can drive the workbench.
	 */
	public DefaultWorkbench(SWTWorkbenchBot bot) {
		this.bot = bot;
	}

	public Workbench switchToPerspective(final String perspectiveName) {
		Boolean result = UIThreadRunnable.syncExec(new Result<Boolean>() {
			public Boolean run() {
				IPerspectiveDescriptor[] perspectives = perspectives();
				for (IPerspectiveDescriptor perspective : perspectives) {
					if (perspectiveNameMatches(perspective, perspectiveName)) {
						activePage().setPerspective(perspective);
						return true;
					}
				}
				return false;
			}
		});
		if (result) {
			return this;
		}
		String availablePerspectives = StringUtils.join(perspectives(), ", ", new StringConverter() {
			public String toString(Object object) {
				return ((IPerspectiveDescriptor) object).getLabel();
			}
		});

		throw new IllegalArgumentException(MessageFormat.format("Cannot select perspective{0}. The valid perspective names are: {1}",//$NON-NLS-1$
				perspectiveName, availablePerspectives));
	}

	public Workbench resetPerspective() {
		UIThreadRunnable.syncExec(new VoidResult() {
			public void run() {
				activePage().resetPerspective();
			}
		});
		return this;
	}

	public Workbench resetWorkbench() {
		return closeAllShells().saveAllEditors().closeAllEditors();
	}

	public Workbench closeAllShells() {
		SWTBotShell[] shells = bot.shells();
		for (SWTBotShell shell : shells) {
			if (!isEclipseShell(shell)) {
				shell.close();
			}
		}
		return this;
	}

	public Workbench saveAllEditors() {
		List<? extends SWTBotEditor> editors = bot.editors();
		for (SWTBotEditor editor : editors) {
			editor.save();
		}
		return this;
	}

	public Workbench closeAllEditors() {
		List<? extends SWTBotEditor> editors = bot.editors();
		for (SWTBotEditor editor : editors) {
			editor.close();
		}
		return this;
	}

	private boolean perspectiveNameMatches(final IPerspectiveDescriptor perspective, String perspectiveName) {
		String perspectiveLabel = perspective.getLabel();
		return perspectiveLabel.equals(perspectiveName) || perspective.getLabel().equals(perspectiveName + " (default)");
	}

	private boolean isEclipseShell(final SWTBotShell shell) {
		return getActiveWorkbenchWindowShell() == shell.widget;
	}

	private IWorkbenchWindow getActiveWorkbenchWindow() {
		return UIThreadRunnable.syncExec(bot.getDisplay(), new Result<IWorkbenchWindow>() {
			public IWorkbenchWindow run() {
				return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			}
		});
	}

	private Widget getActiveWorkbenchWindowShell() {
		return getActiveWorkbenchWindow().getShell();
	}

	private IWorkbenchPage activePage() {
		return getActiveWorkbenchWindow().getActivePage();
	}

	private IPerspectiveDescriptor[] perspectives() {
		return PlatformUI.getWorkbench().getPerspectiveRegistry().getPerspectives();
	}

}
