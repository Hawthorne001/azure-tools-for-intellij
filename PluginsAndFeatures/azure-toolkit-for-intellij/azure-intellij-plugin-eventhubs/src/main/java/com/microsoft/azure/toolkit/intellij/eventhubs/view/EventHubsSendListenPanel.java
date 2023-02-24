package com.microsoft.azure.toolkit.intellij.eventhubs.view;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextField;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler;
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijAzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.eventhubs.EventHubsInstance;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Objects;

public class EventHubsSendListenPanel extends JPanel {
    @Getter
    private JPanel contentPanel;
    private JButton sendMessageBtn;
    private JBTextField messageInput;
    private JPanel listenPanel;
    private JPanel sendPanel;
    private final EventHubsInstance instance;
    private final ConsoleView consoleView;
    private RunProcessHandler listenProcessHandler;

    public EventHubsSendListenPanel(Project project, EventHubsInstance eventHubsInstance) {
        super();
        this.consoleView = new ConsoleViewImpl(project, true);
        this.instance = eventHubsInstance;
        $$$setupUI$$$();
        this.init();
        this.sendMessageBtn.addActionListener(e -> sendMessage());
        this.messageInput.addActionListener(e -> sendMessage());
    }

    public void startListeningProcess() {
        if (Objects.nonNull(this.listenProcessHandler)) {
            return;
        }
        this.listenProcessHandler = new RunProcessHandler();
        listenProcessHandler.addDefaultListener();
        listenProcessHandler.startNotify();
        final ConsoleMessager messager = new ConsoleMessager(consoleView);
        consoleView.attachToProcess(listenProcessHandler);
        final Runnable execute = () -> {
            OperationContext.current().setMessager(messager);
            instance.startListening();
        };
        final Disposable subscribe = Mono.fromRunnable(execute)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
        listenProcessHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void processTerminated(@Nonnull ProcessEvent event) {
                subscribe.dispose();
            }
        });
    }

    public void stopListeningProcess() {
        this.instance.stopListening();
        this.listenProcessHandler.notifyComplete();
        this.listenProcessHandler = null;
    }

    private void init() {
        final GridLayoutManager layout = new GridLayoutManager(1, 1);
        this.setLayout(layout);
        this.add(this.contentPanel, new GridConstraints(0, 0, 1, 1, 0, GridConstraints.ALIGN_FILL, 3, 3, null, null, null, 0));
        this.listenPanel.add(this.consoleView.getComponent(),
                new GridConstraints(0, 0, 1, 1, 0, GridConstraints.ALIGN_FILL,
                        3, 3, null, null, null, 0));
        this.sendMessageBtn.setEnabled(this.instance.isActive());
    }

    @AzureOperation(name = "user/eventhubs.send_message")
    private void sendMessage() {
        final String message = messageInput.getText();
        messageInput.setText(StringUtils.EMPTY);
        AzureTaskManager.getInstance().runInBackground("sending message", () -> {
            this.consoleView.print("Sending message to event hub ...\n", ConsoleViewContentType.LOG_DEBUG_OUTPUT);
            if (this.instance.sendMessage(message)) {
                this.consoleView.print("Successfully send message ", ConsoleViewContentType.LOG_DEBUG_OUTPUT);
                this.consoleView.print(String.format("\"%s\"", message), ConsoleViewContentType.LOG_INFO_OUTPUT);
                this.consoleView.print(" to event hub\n", ConsoleViewContentType.LOG_DEBUG_OUTPUT);
            } else {
                this.consoleView.print("Fail > send message to event hub\n", ConsoleViewContentType.ERROR_OUTPUT);
            }
        });
    }

    private void $$$setupUI$$$() {
    }

    private void createUIComponents() {
        final GridLayoutManager layout = new GridLayoutManager(1, 1);
        this.listenPanel = new JPanel(layout);
    }

    private static class ConsoleMessager extends IntellijAzureMessager {
        private final ConsoleView view;
        public ConsoleMessager(ConsoleView view) {
            super();
            this.view = view;
        }
        @Override
        public boolean show(IAzureMessage raw) {
            if (raw.getType() == IAzureMessage.Type.INFO) {
                view.print(raw.getMessage().toString(), ConsoleViewContentType.SYSTEM_OUTPUT);
                return true;
            } else if (raw.getType() == IAzureMessage.Type.SUCCESS) {
                view.print(raw.getMessage().toString(), ConsoleViewContentType.LOG_INFO_OUTPUT);
                return true;
            } else if (raw.getType() == IAzureMessage.Type.DEBUG) {
                view.print(raw.getMessage().toString(), ConsoleViewContentType.LOG_DEBUG_OUTPUT);
                return true;
            } else if (raw.getType() == IAzureMessage.Type.WARNING) {
                view.print(raw.getMessage().toString(), ConsoleViewContentType.LOG_WARNING_OUTPUT);
            } else if (raw.getType() == IAzureMessage.Type.ERROR) {
                view.print(raw.getMessage().toString(), ConsoleViewContentType.ERROR_OUTPUT);
            }
            return super.show(raw);
        }

    }
}
