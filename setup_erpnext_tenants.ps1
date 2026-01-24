# PowerShell Script to setup ERPNext Multi-Tenancy Sites via Docker
# WARNING: This requires the 'erpnext-backend' container to be running and Mariadb root password.

$DB_ROOT_PASS = "erpnext" # Updated based on container env
$ADMIN_PASS = "admin"

$SITES = @("tenant1-test.localhost", "tenant1-prod.localhost", "tenant2-test.localhost", "tenant2-prod.localhost")

Write-Host "Checking for erpnext-backend container..."
$containerId = docker ps -q -f name=erpnext-backend

if (-not $containerId) {
    Write-Error "erpnext-backend container not found! Please start ERPNext first."
    exit 1
}

foreach ($site in $SITES) {
    Write-Host "Creating site: $site ..."
    
    # Run bench new-site via docker exec
    # Note: This might fail if the site already exists. 
    docker exec erpnext-backend bench new-site $site --admin-password $ADMIN_PASS --db-root-password $DB_ROOT_PASS --install-app erpnext
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Site $site created successfully."
    } else {
        Write-Host "Failed to create site $site (or it already exists)."
    }
}

Write-Host "Multi-Tenancy Setup Complete."
Write-Host "IMPORTANT: Please add the following to your C:\Windows\System32\drivers\etc\hosts file:"
foreach ($site in $SITES) {
    Write-Host "127.0.0.1 $site"
}
