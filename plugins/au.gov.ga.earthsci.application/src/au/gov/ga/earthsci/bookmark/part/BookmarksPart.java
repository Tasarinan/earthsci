/*******************************************************************************
 * Copyright 2012 Geoscience Australia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package au.gov.ga.earthsci.bookmark.part;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.e4.ui.workbench.swt.modeling.EMenuService;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import au.gov.ga.earthsci.bookmark.model.Bookmarks;
import au.gov.ga.earthsci.bookmark.model.IBookmark;
import au.gov.ga.earthsci.bookmark.model.IBookmarkList;
import au.gov.ga.earthsci.worldwind.common.util.Util;

/**
 * A part used to display current bookmarks, and to allow the user to interact with them.
 * 
 * @author James Navin (james.navin@ga.gov.au)
 */
public class BookmarksPart
{
	@Inject
	private Bookmarks bookmarks;
	
	@Inject
	private ESelectionService selectionService;
	
	@Inject
	private IEclipseContext context;
	
	private TableViewer bookmarkListTableViewer;
	private ComboViewer bookmarkListsComboViewer;
	
	@PostConstruct
	public void init(final Composite parent, final MPart part, final EMenuService menuService)
	{
		parent.setLayout(new GridLayout(1, true));
		
		initCombo(parent);
		initList(parent);
		
		context.set(TableViewer.class, bookmarkListTableViewer);
		
		setupBookmarkListInput();
		
		menuService.registerContextMenu(bookmarkListTableViewer.getTable(), "au.gov.ga.earthsci.application.bookmarks.popupmenu"); //$NON-NLS-1$
	}

	private void setupBookmarkListInput()
	{
		bookmarkListTableViewer.setInput(BeanProperties.list("bookmarks").observe(getSelectedBookmarkList())); //$NON-NLS-1$
		bookmarkListsComboViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{
			@Override
			public void selectionChanged(SelectionChangedEvent event)
			{
				bookmarkListTableViewer.setInput(BeanProperties.list("bookmarks").observe(getSelectedBookmarkList())); //$NON-NLS-1$
			}
		});
	}

	private void initCombo(Composite parent)
	{
		bookmarkListsComboViewer = new ComboViewer(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = false;
		bookmarkListsComboViewer.getCombo().setLayoutData(gd);
		
		bookmarkListsComboViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element)
			{
				if (!(element instanceof IBookmarkList))
				{
					return super.getText(element);
				}
				return ((IBookmarkList)element).getName();
			}
		});
		
		bookmarkListsComboViewer.setContentProvider(new ObservableListContentProvider());
		bookmarkListsComboViewer.setInput(BeanProperties.list("lists").observe(bookmarks)); //$NON-NLS-1$
		bookmarkListsComboViewer.setSelection(new StructuredSelection(bookmarks.getDefaultList()));
	}
	
	private void initList(Composite parent)
	{
		Composite tableHolder = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		gd.horizontalAlignment = GridData.FILL;
		tableHolder.setLayoutData(gd);
		
		TableColumnLayout layout = new TableColumnLayout();
		tableHolder.setLayout(layout);
		
		bookmarkListTableViewer = new TableViewer(tableHolder, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		bookmarkListTableViewer.getTable().setHeaderVisible(false);
		bookmarkListTableViewer.getTable().setLinesVisible(false);
		
		bookmarkListTableViewer.getTable().setLayoutData(gd);
		
		TableViewerColumn column = new TableViewerColumn(bookmarkListTableViewer, SWT.LEFT);
		column.setEditingSupport(new BookmarkNameEditingSupport(bookmarkListTableViewer));
		column.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(ViewerCell cell)
			{
				cell.setText(((IBookmark)cell.getElement()).getName());
			}
		});
		ColumnLayoutData cld = new ColumnWeightData(12);
		layout.setColumnData(column.getColumn(), cld);
		
		bookmarkListTableViewer.setContentProvider(new ObservableListContentProvider());
		
		// Allow edit (rename) only via programmatic access (rename command) 
		ColumnViewerEditorActivationStrategy activationStrategy = new ColumnViewerEditorActivationStrategy(bookmarkListTableViewer) {
			@Override
			protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event)
			{
				return event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
			}
		};
		TableViewerEditor.create(bookmarkListTableViewer, activationStrategy, ColumnViewerEditor.KEYBOARD_ACTIVATION);
		
		// Popuplate the current selection with the actual bookmark items
		bookmarkListTableViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{
			@Override
			public void selectionChanged(SelectionChangedEvent event)
			{
				IStructuredSelection selection = (IStructuredSelection) bookmarkListTableViewer.getSelection();
				List<?> list = selection.toList();
				selectionService.setSelection(list.toArray(new IBookmark[list.size()]));
			}
		});
	}
	
	/**
	 * @return The currently selected bookmark list from the combo box
	 */
	private IBookmarkList getSelectedBookmarkList()
	{
		return (IBookmarkList)((StructuredSelection)bookmarkListsComboViewer.getSelection()).getFirstElement();
	}
	
	/**
	 * A simple {@link EditingSupport} implementation that provides in-place editing of bookmark names within the list
	 */
	private static class BookmarkNameEditingSupport extends EditingSupport
	{
		private TableViewer viewer;
		
		public BookmarkNameEditingSupport(TableViewer viewer)
		{
			super(viewer);
			this.viewer = viewer;
		}

		@Override
		protected CellEditor getCellEditor(Object element)
		{
			return new TextCellEditor(viewer.getTable());
		}

		@Override
		protected boolean canEdit(Object element)
		{
			return true;
		}

		@Override
		protected Object getValue(Object element)
		{
			return ((IBookmark)element).getName();
		}

		@Override
		protected void setValue(Object element, Object value)
		{
			String stringValue = (String)value;
			if (!Util.isBlank(stringValue))
			{
				((IBookmark)element).setName(stringValue);
			}
			viewer.update(element, null);
		}
		
	}
}