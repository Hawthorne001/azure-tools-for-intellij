package com.microsoft.azure.toolkit.ide.hdinsight.spark;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzureServiceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.hdinsight.spark.component.SparkClusterNodeView;
import com.microsoft.azure.toolkit.ide.hdinsight.spark.component.SparkJobNodeView;
import com.microsoft.azure.toolkit.lib.hdinsight.AzureHDInsightService;
import com.microsoft.azure.toolkit.lib.hdinsight.SparkCluster;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class HDInsightNodeProvider implements IExplorerNodeProvider {

    private static final String NAME = "HDInsight";
    private static final String ICON = AzureIcons.HDInsight.MODULE.getIconPath();

    @Nullable
    @Override
    public Object getRoot() {
        return az(AzureHDInsightService.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureHDInsightService
                || data instanceof SparkCluster;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data,@Nullable Node<?> parent,@Nonnull Manager manager) {
        if (data instanceof AzureHDInsightService) {
            AzureHDInsightService service = (AzureHDInsightService)data;
            Function<AzureHDInsightService, List<SparkCluster>> clusters = s -> s.list().stream()
                    .flatMap(m -> m.clusters().list().stream()).collect(Collectors.toList());
            return new Node<>(service).view(new AzureServiceLabelView(service, "HDInsight", ICON))
                    .actions("actions.hdinsight.service")
                    .addChildren(clusters, (cluster, serviceNode) -> this.createNode(cluster, serviceNode, manager));
        } else if (data instanceof SparkCluster){
            final SparkCluster sparkCluster = (SparkCluster) data;
            Node<SparkCluster> jobsNode = new Node<>(sparkCluster)
                    .view(new SparkJobNodeView(sparkCluster))
                    .clickAction(HDInsightActionsContributor.OPEN_HDINSIGHT_JOB_VIEW);
            return new Node<>(sparkCluster)
                        .view(new SparkClusterNodeView(sparkCluster))
                        .inlineAction(ResourceCommonActionsContributor.PIN)
                        .actions(HDInsightActionsContributor.SPARK_CLUSTER_ACTIONS)
                        .addChild(jobsNode);
        } else {
            return null;
        }
    }

}