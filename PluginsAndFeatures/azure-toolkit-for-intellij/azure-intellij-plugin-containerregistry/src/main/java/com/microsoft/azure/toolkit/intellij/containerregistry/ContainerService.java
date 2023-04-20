package com.microsoft.azure.toolkit.intellij.containerregistry;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.core.command.PushImageResultCallback;
import com.microsoft.azure.toolkit.intellij.container.DockerUtil;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContainerService {
    private static final ContainerService instance = new ContainerService();

    public static ContainerService getInstance() {
        return ContainerService.instance;
    }

    public String pushDockerImage(@Nonnull final IDockerPushConfiguration configuration) throws InterruptedException {
        final IAzureMessager messager = AzureMessager.getMessager();
        final DockerImage image = configuration.getDockerImageConfiguration();
        final DockerClient dockerClient = DockerUtil.getDockerClient(Objects.requireNonNull(configuration.getDockerHostConfiguration()));
        final ContainerRegistry registry = Azure.az(AzureContainerRegistry.class).getById(configuration.getContainerRegistryId());
        final String loginServerUrl = Objects.requireNonNull(registry).getLoginServerUrl();
        final String fullRepositoryName = StringUtils.startsWith(Objects.requireNonNull(image).getRepositoryName(), loginServerUrl) ? image.getRepositoryName() : loginServerUrl + "/" + image.getRepositoryName();
        final String imageAndTag = fullRepositoryName + ":" + ObjectUtils.defaultIfNull(image.getTagName(), "latest");
        // tag image with ACR url
        if (!StringUtils.startsWith(image.getImageName(), loginServerUrl)) {
            DockerUtil.tagImage(dockerClient, image.getImageName(), fullRepositoryName, image.getTagName());
        }
        // push to ACR
        messager.info(String.format("Pushing to ACR ... [%s] ", loginServerUrl));
        final PushImageResultCallback callBack = new PushImageResultCallback() {
            @Override
            public void onNext(PushResponseItem item) {
                final String message = Stream.of(item.getStatus(), item.getId(), item.getProgress()).filter(StringUtils::isNoneBlank).collect(Collectors.joining(" "));
                messager.info(message);
                super.onNext(item);
            }
        };
        DockerUtil.pushImage(dockerClient, Objects.requireNonNull(loginServerUrl), registry.getUserName(), registry.getPrimaryCredential(), imageAndTag, callBack);
        return loginServerUrl;
    }
}
