using RabbitmqDotnetClient.Rabbit;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddSingleton(RabbitOptions.Load(builder.Configuration));
builder.Services.AddSingleton<RabbitConnection>();
builder.Services.AddHostedService<RabbitConsumerService>();

var app = builder.Build();

app.MapPost("/api/messages", async (SendMessageRequest request, RabbitConnection rabbit, CancellationToken ct) =>
{
    await rabbit.PublishAsync(request.Message, ct);
    return Results.Ok(new { status = "sent", request.Message });
});

app.Run();

internal record SendMessageRequest(string Message);
