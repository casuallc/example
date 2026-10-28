using System.Text;
using RabbitMQ.Client;

namespace RabbitmqDotnetClient.Rabbit;

/// <summary>
/// Owns the shared RabbitMQ connection and the channel used for publishing.
/// Initialization is lazy and idempotent so the consumer hosted service and the
/// HTTP endpoint can start in any order.
/// </summary>
public sealed class RabbitConnection : IAsyncDisposable
{
    private readonly RabbitOptions _options;
    private readonly ILogger<RabbitConnection> _logger;
    private readonly SemaphoreSlim _initLock = new(1, 1);

    private IConnection? _connection;
    private IChannel? _publishChannel;

    public RabbitConnection(RabbitOptions options, ILogger<RabbitConnection> logger)
    {
        _options = options;
        _logger = logger;
    }

    public async Task EnsureInitializedAsync(CancellationToken ct = default)
    {
        if (_connection is not null)
        {
            return;
        }
        await _initLock.WaitAsync(ct);
        try
        {
            if (_connection is not null)
            {
                return;
            }
            var factory = new ConnectionFactory
            {
                HostName = _options.Host,
                Port = _options.Port,
                UserName = _options.Username,
                Password = _options.Password,
                VirtualHost = _options.VirtualHost,
            };
            _connection = await factory.CreateConnectionAsync(ct);
            _publishChannel = await _connection.CreateChannelAsync(cancellationToken: ct);
            await DeclareTopologyAsync(_publishChannel, ct);
            _logger.LogInformation(
                "Connected to RabbitMQ {Host}:{Port} vhost={VirtualHost}",
                _options.Host, _options.Port, _options.VirtualHost);
        }
        finally
        {
            _initLock.Release();
        }
    }

    public async Task<IChannel> CreateChannelAsync(CancellationToken ct = default)
    {
        await EnsureInitializedAsync(ct);
        return await _connection!.CreateChannelAsync(cancellationToken: ct);
    }

    /// <summary>Declares the shared demo topology; safe to call repeatedly.</summary>
    public async Task DeclareTopologyAsync(IChannel channel, CancellationToken ct = default)
    {
        await channel.ExchangeDeclareAsync(
            _options.Exchange, ExchangeType.Topic, durable: true, autoDelete: false, cancellationToken: ct);
        await channel.QueueDeclareAsync(
            _options.Queue, durable: true, exclusive: false, autoDelete: false, cancellationToken: ct);
        await channel.QueueBindAsync(_options.Queue, _options.Exchange, _options.RoutingKey, cancellationToken: ct);
    }

    public async Task PublishAsync(string message, CancellationToken ct = default)
    {
        await EnsureInitializedAsync(ct);
        var properties = new BasicProperties
        {
            ContentType = "text/plain",
            DeliveryMode = DeliveryModes.Persistent,
        };
        var body = Encoding.UTF8.GetBytes(message);
        await _publishChannel!.BasicPublishAsync(
            _options.Exchange, _options.RoutingKey, mandatory: false, basicProperties: properties, body: body,
            cancellationToken: ct);
        _logger.LogInformation(
            "Sent message to exchange={Exchange} routingKey={RoutingKey}: {Message}",
            _options.Exchange, _options.RoutingKey, message);
    }

    public async ValueTask DisposeAsync()
    {
        if (_publishChannel is not null)
        {
            await _publishChannel.DisposeAsync();
        }
        if (_connection is not null)
        {
            await _connection.DisposeAsync();
        }
        _initLock.Dispose();
    }
}
