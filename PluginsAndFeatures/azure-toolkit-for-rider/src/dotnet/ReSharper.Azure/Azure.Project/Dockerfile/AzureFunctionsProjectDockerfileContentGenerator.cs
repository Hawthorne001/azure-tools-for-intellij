using System.Text;
using JetBrains.Application.Parts;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Azure.Project.FunctionApp;
using JetBrains.Rider.Backend.Features.Docker.Generation;
using JetBrains.Util;
using JetBrains.Util.Dotnet.TargetFrameworkIds;

namespace JetBrains.ReSharper.Azure.Project.Dockerfile;

[SolutionComponent(Instantiation.DemandAnyThreadSafe)]
public class AzureFunctionsProjectDockerfileContentGenerator(ILogger logger) : IDotNetProjectDockerfileContentGenerator
{
    public int Priority => 10;

    public bool IsApplicable(IProject project) => project.IsAzureFunctionsProject();

    public string Generate(
        IProject project,
        TargetFrameworkId targetFrameworkId,
        DockerProjectType? projectType,
        VirtualFileSystemPath dockerfileContextPath)
    {
        logger.Trace("Generating Dockerfile content for Azure Functions project");

        var content = new StringBuilder();



        return content.ToString();
    }
}