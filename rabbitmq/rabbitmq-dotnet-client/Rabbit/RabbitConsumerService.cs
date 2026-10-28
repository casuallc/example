using System.Text;
using RabbitMQ.Client;
using RabbitMQ.Client.Events;

namespace RabbitmqDotnetClient.Rabbit;

/// <summary>
/// Background consumer on the shared demo queue. Declares the topology itself
/// (idempotent) so the sample works even if nothing has published yet.
/// </summary>
public sealed class RabbitConsumerService : BackgroundService
{
    private readonly RabbitConnection _rabbit;
    private readonly RabbitOptions _options;
    private readonly ILogger<RabbitConsumerService> _logger;

    public RabbitConsumerService(
        RabbitConnection rabbit, RabbitOptions options, ILogger<RabbitConsumerService> logger)
    {
        _rabbit = rabbit;
        _options = options;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        await _rabbit.EnsureInitializedAsync(stoppingToken);
        await using var channel = await _rabbit.CreateChannelAsync(stoppingToken);
        await _rabbit.DeclareTopologyAsync(channel, stoppingToken);

        var consumer = new AsyncEventingBasicConsumer(channel);
        consumer.ReceivedAsync += async (_, eventArgs) =>
        {
            var message = Encoding.UTF8.GetString(eventArgs.Body.Span);
            _logger.LogInformation("Received message: {Message}", message);
            await channel.BasicAckAsync(eventArgs.DeliveryTag, multiple: false, stoppingToken);
        };
        await channel.BasicConsumeAsync(_options.Queue, autoAck: false, consumer, stoppingToken);
        _logger.LogInformation(
            "Consuming queue={Queue} bound to exchange={Exchange} routingKey={RoutingKey}",
            _options.Queue, _options.Exchange, _options.RoutingKey);

        await Task.Delay(Timeout.Infinite, stoppingToken);
    }
}
