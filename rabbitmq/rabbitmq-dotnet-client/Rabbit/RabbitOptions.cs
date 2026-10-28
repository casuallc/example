namespace RabbitmqDotnetClient.Rabbit;

/// <summary>
/// Connection/topology options. Values come from appsettings.json ("RabbitMQ" section,
/// defaults point at the test environment) and can be overridden by the
/// RABBITMQ_HOST / RABBITMQ_PORT / RABBITMQ_USERNAME / RABBITMQ_PASSWORD / RABBITMQ_VHOST
/// environment variables.
/// </summary>
public sealed class RabbitOptions
{
    public string Host { get; init; } = "172.20.140.143";
    public int Port { get; init; } = 5672;
    public string Username { get; init; } = "admq";
    public string Password { get; init; } = "changeme";
    public string VirtualHost { get; init; } = "apusic";
    public string Exchange { get; init; } = "demo.topic";
    public string Queue { get; init; } = "demo.queue";
    public string RoutingKey { get; init; } = "demo.message";

    public static RabbitOptions Load(IConfiguration config)
    {
        var section = config.GetSection("RabbitMQ");
        return new RabbitOptions
        {
            Host = Env("RABBITMQ_HOST", section["Host"]) ?? "172.20.140.143",
            Port = int.TryParse(Env("RABBITMQ_PORT", section["Port"]), out var port) ? port : 5672,
            Username = Env("RABBITMQ_USERNAME", section["Username"]) ?? "admq",
            Password = Env("RABBITMQ_PASSWORD", section["Password"]) ?? "changeme",
            VirtualHost = Env("RABBITMQ_VHOST", section["VirtualHost"]) ?? "apusic",
            Exchange = section["Exchange"] ?? "demo.topic",
            Queue = section["Queue"] ?? "demo.queue",
            RoutingKey = section["RoutingKey"] ?? "demo.message",
        };
    }

    private static string? Env(string name, string? fallback) =>
        Environment.GetEnvironmentVariable(name) is { Length: > 0 } value ? value : fallback;
}
