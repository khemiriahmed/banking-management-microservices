param(
    [Parameter(Mandatory = $true)]
    [string]$AccountId,
    [string]$Region = 'eu-west-3',
    [string]$Profile = 'banking-demo',
    [string]$Version = (git rev-parse --short HEAD)
)

$ErrorActionPreference = 'Stop'

$registry = "$AccountId.dkr.ecr.$Region.amazonaws.com"
$images = @(
    @{ Name = 'config-server'; Context = 'backend/config-server' },
    @{ Name = 'discovery-server'; Context = 'backend/discovery-server' },
    @{ Name = 'api-gateway'; Context = 'backend/api-gateway' },
    @{ Name = 'demo-service'; Context = 'backend/demo-service' },
    @{ Name = 'auth-service'; Context = 'backend/auth-service' },
    @{ Name = 'user-service'; Context = 'backend/user-service' },
    @{ Name = 'account-service'; Context = 'backend/account-service' },
    @{ Name = 'transfer-service'; Context = 'backend/transfer-service' },
    @{ Name = 'transaction-service'; Context = 'backend/transaction-service' },
    @{ Name = 'notification-service'; Context = 'backend/notification-service' },
    @{ Name = 'audit-service'; Context = 'backend/audit-service' },
    @{ Name = 'frontend'; Context = 'frontend' }
)

aws ecr get-login-password --region $Region --profile $Profile | docker login --username AWS --password-stdin $registry

foreach ($image in $images) {
    $repository = "banking/$($image.Name)"
    aws ecr describe-repositories --repository-names $repository --region $Region --profile $Profile 2>$null
    if ($LASTEXITCODE -ne 0) {
        aws ecr create-repository --repository-name $repository --region $Region --profile $Profile | Out-Null
    }
    $target = "$registry/$repository"
    docker build -t "$target`:$Version" -t "$target`:latest" $image.Context
    docker push "$target`:$Version"
    docker push "$target`:latest"
}

Write-Output "Pushed $($images.Count) images with version $Version"
