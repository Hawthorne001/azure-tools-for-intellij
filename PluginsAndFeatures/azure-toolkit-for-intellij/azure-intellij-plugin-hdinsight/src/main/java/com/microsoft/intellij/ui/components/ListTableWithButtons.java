/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui.components;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.*;
import com.intellij.ui.table.TableView;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListTableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;

/**
 * This file is based on code in IntelliJ-Community repository, please refer to link below.
 * https://github.com/JetBrains/intellij-community/blob/6e59e93c07a2856995d3d5c76c94afd744de33bb/platform/lang-api/src/com/intellij/execution/util/ListTableWithButtons.java
 * TODO: We're supposed to remove this file and replace this class with `com.intellij.execution.util.ListTableWithButtons` when IntelliJ upgrade to 2019.1
 */
public abstract class ListTableWithButtons<T> extends Observable {
    private final List<T> myElements = ContainerUtil.newArrayList();
    private JPanel myPanel;
    private final TableView<T> myTableView;
    private final CommonActionsPanel myActionsPanel;
    private boolean myIsEnabled = true;
    private final ToolbarDecorator myDecorator;

    protected ListTableWithButtons() {
        myTableView = new TableView<T>(createListModel()) {
            @NotNull
            public AnActionEvent createEmptyEvent() {
                return AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, null, dataId -> null);
            }

            @Override
            protected void createDefaultEditors() {
                super.createDefaultEditors();
                Object editor = defaultEditorsByColumnClass.get(String.class);
                if (editor instanceof DefaultCellEditor) {
                    ((DefaultCellEditor)editor).getComponent().addKeyListener(new KeyAdapter() {
                        @Override
                        public void keyPressed(KeyEvent e) {
                            final int column = myTableView.getEditingColumn();
                            final int row = myTableView.getEditingRow();
                            if (e.getModifiers() == 0 && (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB)) {
                                e.consume();
                                SwingUtilities.invokeLater(() -> {
                                    stopEditing();
                                    int nextColumn = column < myTableView.getColumnCount() - 1 ? column + 1 : 0;
                                    int nextRow = nextColumn == 0 ? row + 1 : row;
                                    if (nextRow > myTableView.getRowCount() - 1) {
                                        if (myElements.isEmpty() || !ListTableWithButtons.this.isEmpty(myElements.get(myElements.size() - 1))) {
                                            AnActionButton addButton = ToolbarDecorator.findAddButton(myPanel);
                                            if (addButton != null) {
                                                addButton.actionPerformed(createEmptyEvent());
                                            }
                                            return;
                                        }
                                        else {
                                            nextRow = 0;
                                        }
                                    }
                                    myTableView.scrollRectToVisible(myTableView.getCellRect(nextRow, nextColumn, true));
                                    myTableView.editCellAt(nextRow, nextColumn);
                                });
                            }
                        }
                    });
                }
            }
        };
        myTableView.setIntercellSpacing(JBUI.emptySize());
        myTableView.setStriped(true);
        myTableView.getTableViewModel().setSortable(false);
        myTableView.getComponent().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        myDecorator = ToolbarDecorator.createDecorator(myTableView);
        myActionsPanel = myDecorator.getActionsPanel();
    }

    @Nullable
    protected AnActionButtonRunnable createRemoveAction() {
        return button -> removeSelected();
    }

    @Nullable
    protected AnActionButtonRunnable createAddAction() {
        return button -> {
            myTableView.stopEditing();
            setModified();
            SwingUtilities.invokeLater(() -> {
                if (myElements.isEmpty() || !isEmpty(myElements.get(myElements.size() - 1))) {
                    myElements.add(createElement());
                    myTableView.getTableViewModel().setItems(myElements);
                }
                myTableView.scrollRectToVisible(myTableView.getCellRect(myElements.size() - 1, 0, true));
                myTableView.getComponent().editCellAt(myElements.size() - 1, 0);
                myTableView.getComponent().revalidate();
                myTableView.getComponent().repaint();
            });
        };
    }

    protected void removeSelected() {
        List<T> selected = getSelection();
        if (!selected.isEmpty()) {
            myTableView.stopEditing();
            setModified();
            int selectedIndex = myTableView.getSelectionModel().getLeadSelectionIndex();
            myTableView.scrollRectToVisible(myTableView.getCellRect(selectedIndex, 0, true));
            selected = ContainerUtil.filter(selected, this::canDeleteElement);
            myElements.removeAll(selected);
            myTableView.getSelectionModel().clearSelection();
            myTableView.getTableViewModel().setItems(myElements);

            int prev = selectedIndex - 1;
            if (prev >= 0) {
                myTableView.getComponent().getSelectionModel().setSelectionInterval(prev, prev);
            }
            else if (selectedIndex < myElements.size()) {
                myTableView.getComponent().getSelectionModel().setSelectionInterval(selectedIndex, selectedIndex);
            }
        }
    }

    @NotNull
    public TableView<T> getTableView() {
        return myTableView;
    }

    protected abstract ListTableModel createListModel();

    protected void setModified() {
        setChanged();
        notifyObservers();
    }

    protected List<T> getElements() {
        return myElements;
    }

    public JComponent getComponent() {
        if (myPanel == null) {
            myPanel = myDecorator
                    .setAddAction(createAddAction())
                    .setRemoveAction(createRemoveAction())
                    .disableUpDownActions().addExtraActions(createExtraActions()).createPanel();

            AnActionButton removeButton = ToolbarDecorator.findRemoveButton(myPanel);
            if (removeButton != null) {
                removeButton.addCustomUpdater(e -> {
                    List<T> selection = getSelection();
                    if (selection.isEmpty() || !myIsEnabled) return false;
                    for (T t : selection) {
                        if (!canDeleteElement(t)) return false;
                    }
                    return true;
                });
            }

            AnActionButton addButton = ToolbarDecorator.findAddButton(myPanel);
            if (addButton != null) {
                addButton.addCustomUpdater(e -> myIsEnabled);
            }
        }
        return myPanel;
    }

    public CommonActionsPanel getActionsPanel() {
        return myActionsPanel;
    }

    public void setEnabled() {
        myTableView.getComponent().setEnabled(true);
        myIsEnabled = true;
    }

    public void setDisabled() {
        myTableView.getComponent().setEnabled(false);
        myIsEnabled = false;
    }

    public void stopEditing() {
        myTableView.stopEditing();
    }

    public void refreshValues() {
        myTableView.getComponent().repaint();
    }

    protected void setSelection(T element) {
        myTableView.setSelection(Collections.singleton(element));
        TableUtil.scrollSelectionToVisible(myTableView);
    }

    protected void editSelection(int column) {
        List<T> selection = getSelection();
        if (selection.size() != 1) return;
        int row = myElements.indexOf(selection.get(0));
        if (row != -1) {
            TableUtil.editCellAt(myTableView, row, column);
        }
    }

    protected abstract T createElement();

    protected abstract boolean isEmpty(T element);

    @NotNull
    protected AnActionButton[] createExtraActions() {
        return new AnActionButton[0];
    }

    @NotNull
    protected List<T> getSelection() {
        int[] selection = myTableView.getComponent().getSelectedRows();
        if (selection.length == 0) {
            return Collections.emptyList();
        }
        else {
            List<T> result = new ArrayList<>(selection.length);
            for (int row : selection) {
                result.add(myElements.get(row));
            }
            return result;
        }
    }

    public void setValues(List<T> envVariables) {
        myElements.clear();
        for (T envVariable : envVariables) {
            myElements.add(cloneElement(envVariable));
        }
        myTableView.getTableViewModel().setItems(myElements);
    }

    protected abstract T cloneElement(T variable);

    protected abstract boolean canDeleteElement(T selection);

    protected abstract static class ElementsColumnInfoBase<T> extends ColumnInfo<T, String> {
        private DefaultTableCellRenderer myRenderer;

        protected ElementsColumnInfoBase(String name) {
            super(name);
        }

        @Override
        public TableCellRenderer getRenderer(T element) {
            if (myRenderer == null) {
                myRenderer = new DefaultTableCellRenderer();
            }
            if (element != null) {
                myRenderer.setText(valueOf(element));
                myRenderer.setToolTipText(getDescription(element));
            }
            return myRenderer;
        }

        @Nullable
        protected abstract String getDescription(T element);
    }
}
