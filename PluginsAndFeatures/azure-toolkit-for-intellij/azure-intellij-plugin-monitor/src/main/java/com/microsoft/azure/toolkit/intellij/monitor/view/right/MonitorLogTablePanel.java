/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.monitor.view.right;

import com.azure.monitor.query.models.LogsTable;
import com.azure.monitor.query.models.LogsTableCell;
import com.azure.monitor.query.models.LogsTableRow;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.AnActionLink;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.intellij.common.TextDocumentListenerAdapter;
import com.microsoft.azure.toolkit.intellij.common.component.HighLightedCellRenderer;
import com.microsoft.azure.toolkit.intellij.monitor.view.left.WorkspaceSelectionDialog;
import com.microsoft.azure.toolkit.intellij.monitor.view.right.filter.KustoFilterComboBox;
import com.microsoft.azure.toolkit.intellij.monitor.view.right.filter.TimeRangeComboBox;
import com.microsoft.azure.toolkit.intellij.monitor.view.right.table.LogTable;
import com.microsoft.azure.toolkit.intellij.monitor.view.right.table.LogTableModel;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspace;
import lombok.Setter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.util.*;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class MonitorLogTablePanel {
    private JPanel contentPanel;
    private JPanel filterPanel;
    private LogTable logTable;
    private TimeRangeComboBox timeRangeComboBox;
    private KustoFilterComboBox resourceComboBox;
    private KustoFilterComboBox levelComboBox;
    private JButton runButton;
    private ActionLink exportAction;
    private SearchTextField searchField;
    private JPanel timeRangePanel;
    private JPanel levelPanel;
    private JPanel resourcePanel;
    private JLabel logLevelLabel;
    private JLabel resourceLabel;
    private final static String[] RESOURCE_COMBOBOX_COLUMN_NAMES = {"_ResourceId", "ResourceId"};
    private final static String[] LEVEL_COMBOBOX_COLUMN = {"Level"};
    private final static String RESULT_CSV_FILE = "result.csv";
    @Setter
    private String initResourceId;

    public MonitorLogTablePanel() {
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.customizeTableUi();
        this.hideFilters();
        this.runButton.setIcon(AllIcons.Actions.Execute);
        AzureEventBus.on("azure.monitor.change_workspace", new AzureEventBus.EventListener(e -> initResourceId = null));
    }

    public JPanel getContentPanel() {
        return this.contentPanel;
    }

    public String getQueryStringFromFilters(String tableName) {
        final List<String> queryParams = new ArrayList<>(Arrays.asList(tableName, timeRangeComboBox.getKustoString()));
        if (Objects.nonNull(initResourceId)) {
            queryParams.add(String.format("where _ResourceId == \"%s\"", initResourceId));
        } else if (resourceLabel.isEnabled() && StringUtils.isNotBlank(resourceComboBox.getKustoString())) {
            queryParams.add(resourceComboBox.getKustoString());
        }
        if (logLevelLabel.isEnabled() && StringUtils.isNotBlank(levelComboBox.getKustoString())) {
            queryParams.add(levelComboBox.getKustoString());
        }
        // display logs with latest time
        queryParams.add("sort by TimeGenerated desc");
        final String rowNumberLimitation = String.format("take %s", Azure.az().config().getMonitorQueryRowNumber());
        queryParams.add(rowNumberLimitation);
        return StringUtils.join(queryParams.stream().filter(StringUtils::isNotBlank).toList(), " | ");
    }

    public void loadTableModel(@Nullable LogAnalyticsWorkspace selectedWorkspace, String queryString) {
        if (Objects.isNull(selectedWorkspace)) {
            AzureMessager.getMessager().info(message("azure.monitor.info.selectWorkspace"), null, selectWorkspaceAction());
            return;
        }
        logTable.clearModel();
        logTable.setLoading(true);
        runButton.setEnabled(false);
        exportAction.setEnabled(false);
        AzureTaskManager.getInstance().runInBackground("load Azure Monitor data", () -> {
            try {
                final LogsTable result = selectedWorkspace.executeQuery(queryString);
                AzureTaskManager.getInstance().runLater(() -> {
                    if (Objects.isNull(result)) {
                        return;
                    }
                    if (result.getAllTableCells().size() > 0) {
                        this.exportAction.setEnabled(true);
                        this.logTable.setModel(result.getRows());
                    }
                }, AzureTask.Modality.ANY);
            } catch (final Exception e) {
                throw new AzureToolkitRuntimeException(e);
            } finally {
                AzureTaskManager.getInstance().runLater(() -> {
                    logTable.setLoading(false);
                    runButton.setEnabled(true);
                }, AzureTask.Modality.ANY);
            }
        });
    }

    public void loadFilters(@Nullable LogAnalyticsWorkspace selectedWorkspace, String tableName) {
        if (Objects.isNull(selectedWorkspace)) {
            AzureMessager.getMessager().info(message("azure.monitor.info.selectWorkspace"), null, selectWorkspaceAction());
            return;
        }
        timeRangePanel.setVisible(true);
        resourcePanel.setVisible(true);
        levelPanel.setVisible(true);
        logLevelLabel.setEnabled(false);
        resourceLabel.setEnabled(false);
        AzureTaskManager.getInstance().runInBackground("load filters", () -> {
            final Map<String, List<String>> result = new HashMap<>();
            try {
                final List<String> tableColumns = queryColumnNameList(selectedWorkspace, tableName);
                final List<String> specificColumnNames = new ArrayList<>(Arrays.asList(RESOURCE_COMBOBOX_COLUMN_NAMES));
                specificColumnNames.addAll(Arrays.asList(LEVEL_COMBOBOX_COLUMN));
                result.putAll(queryCellValueList(selectedWorkspace, tableName, specificColumnNames, tableColumns));
            } catch (final Exception e) {
                throw new AzureToolkitRuntimeException(e);
            } finally {
                AzureTaskManager.getInstance().runLater(() -> updateCombobox(result), AzureTask.Modality.ANY);
            }
        });
    }

    public void addTableSelectionListener(ListSelectionListener selectionListener) {
        this.logTable.getColumnModel().getSelectionModel().addListSelectionListener(selectionListener);
        this.logTable.getSelectionModel().addListSelectionListener(selectionListener);
    }

    public void addRunActionListener(ActionListener listener) {
        this.runButton.addActionListener(listener);
    }

    @Nullable
    public String getSelectedCellValue() {
        final Object value = this.logTable.getValueAt(this.logTable.getSelectedRow(), this.logTable.getSelectedColumn());
        return Optional.ofNullable(value).map(Object::toString).orElse(StringUtils.EMPTY);
    }

    @Nullable
    public String getSelectedColumnName() {
        return this.logTable.getColumnName(this.logTable.getSelectedColumn());
    }

    private void customizeTableUi() {
        this.logTable.setDefaultRenderer(String.class, new HighLightedCellRenderer(searchField.getTextEditor()));
        this.logTable.setFont(JBUI.Fonts.create("JetBrains Mono", 12));
        this.logTable.getTableHeader().setFont(JBUI.Fonts.create("JetBrains Mono", 12));
        searchField.addDocumentListener((TextDocumentListenerAdapter) () -> logTable.filter(searchField.getText()));
    }

    private void updateCombobox(Map<String, List<String>> map) {
        resourcePanel.setVisible(false);
        levelPanel.setVisible(false);
        Arrays.stream(RESOURCE_COMBOBOX_COLUMN_NAMES).filter(map::containsKey).findFirst()
                .ifPresent(it -> {
                    final List<String> items = map.get(it);
                    Optional.ofNullable(initResourceId).ifPresent(resourceId -> {
                        if (!items.contains(initResourceId)) {
                            items.add(initResourceId);
                        }
                    });
                    updateComboboxItems(resourcePanel, items, it);
                    resourceComboBox.setValue(Objects.isNull(initResourceId) ? KustoFilterComboBox.ALL : initResourceId);
                });
        Arrays.stream(LEVEL_COMBOBOX_COLUMN).filter(map::containsKey).findFirst()
                .ifPresent(it -> {
                    updateComboboxItems(levelPanel, map.get(it), it);
                    levelComboBox.setValue(KustoFilterComboBox.ALL);
                });
    }

    private void updateComboboxItems(JPanel panel, List<String> items, String key) {
        if (items.size() <=0 ) {
            return;
        }
        panel.setVisible(true);
        panel.getComponent(0).setEnabled(true);
        final KustoFilterComboBox comboBox = (KustoFilterComboBox) panel.getComponent(1);
        comboBox.setItemsLoader(() -> {
            final List<String> result = new ArrayList<>();
            result.add(KustoFilterComboBox.ALL);
            result.addAll(items);
            return result;
        });
        comboBox.setColumnName(key);
        comboBox.reloadItems();
    }

    private List<String> queryColumnNameList(LogAnalyticsWorkspace selectedWorkspace, String tableName) {
        return Optional.ofNullable(selectedWorkspace.executeQuery(String.format("%s | take 1", tableName)))
                .map(LogsTable::getAllTableCells).orElse(new ArrayList<>())
                .stream().map(LogsTableCell::getColumnName).toList();
    }

    private Map<String, List<String>> queryCellValueList(LogAnalyticsWorkspace selectedWorkspace, String tableName,
                                                          List<String> specificColumnNames, List<String> columnNamesInTable) {
        final Map<String, List<String>> result = new HashMap<>();
        final String kustoColumnNames = StringUtils.join(specificColumnNames.stream()
                .filter(columnNamesInTable::contains).toList(), ",");
        if (StringUtils.isBlank(kustoColumnNames)) {
            return result;
        }
        final String queryString = String.format("%s | distinct %s | project %s", tableName, kustoColumnNames, kustoColumnNames);
        Optional.ofNullable(selectedWorkspace.executeQuery(queryString))
                .map(LogsTable::getAllTableCells).orElse(new ArrayList<>()).forEach(logsTableCell -> {
                    if (!result.containsKey(logsTableCell.getColumnName())) {
                        result.put(logsTableCell.getColumnName(), new ArrayList<>());
                    }
                    result.get(logsTableCell.getColumnName()).add(logsTableCell.getValueAsString());
                });
        return result;
    }

    private void hideFilters() {
        this.timeRangePanel.setVisible(false);
        this.resourcePanel.setVisible(false);
        this.levelPanel.setVisible(false);
    }

    @AzureOperation(name = "user/monitor.export_query_result")
    private void exportQueryResult() {
        final FileSaverDescriptor fileDescriptor = new FileSaverDescriptor(message("azure.monitor.export.description"), "");
        final FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(fileDescriptor, (Project) null);
        final VirtualFile userHome = LocalFileSystem.getInstance().findFileByPath(System.getProperty("user.home"));
        final VirtualFileWrapper fileWrapper = dialog.save(userHome, RESULT_CSV_FILE);
        Optional.ofNullable(fileWrapper).map(VirtualFileWrapper::getFile).ifPresent(it ->
                AzureTaskManager.getInstance().runInBackground("Export query data", () -> exportTableData(it, logTable.getLogTableModel())));
    }

    private void exportTableData(File target, LogTableModel tableModel) {
        try {
            if (target == null) {
                return;
            }
            final File parentFolder = target.getParentFile();
            if (!parentFolder.exists()) {
                parentFolder.mkdirs();
            }
            if (!target.exists()) {
                target.createNewFile();
            }
            final CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(target),
                    CSVFormat.Builder.create().setHeader(tableModel.getColumnNames().toArray(new String[0])).build());
            for (final LogsTableRow row : tableModel.getLogsTableRows()) {
                csvPrinter.printRecord(row.getRow().stream().map(LogsTableCell::getValueAsString).toList());
            }
            csvPrinter.close();
            AzureMessager.getMessager().info(message("azure.monitor.export.succeed.message", target.getAbsolutePath()),
                    message("azure.monitor.export.succeed.title"));
        } catch (final Exception e) {
            throw new AzureToolkitRuntimeException(e);
        }
    }

    private Action<?> selectWorkspaceAction() {
        final Project project = ProjectManager.getInstance().getDefaultProject();
        return new Action<>(Action.Id.of("user/monitor.select_workspace"))
            .withLabel("Select")
            .withHandler((s, e) -> AzureTaskManager.getInstance().runLater(() -> {
                final WorkspaceSelectionDialog dialog = new WorkspaceSelectionDialog(project, null);
                if (dialog.showAndGet()) {
                    Optional.ofNullable(dialog.getWorkspace()).ifPresent(w -> AzureEventBus.emit("azure.monitor.change_workspace", w));
                }
            }));
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    private void createUIComponents() {
        this.logTable = new LogTable();
        this.timeRangeComboBox = new TimeRangeComboBox();
        this.exportAction = new AnActionLink("Export", new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                exportQueryResult();
            }
        });
        this.exportAction.setExternalLinkIcon();
        this.exportAction.setAutoHideOnDisable(false);
        this.resourceComboBox = new KustoFilterComboBox();
        this.levelComboBox = new KustoFilterComboBox() {
            @Override
            protected String getItemText(Object item) {
                return Objects.nonNull(item) ? item.toString() : StringUtils.EMPTY;
            }
        };
    }
}
