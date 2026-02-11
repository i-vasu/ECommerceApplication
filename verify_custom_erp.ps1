$ErrorActionPreference = "Stop"

# Test Custom ERP product seeding
Write-Host "Verifying Custom ERP Setup..." -ForegroundColor Cyan

try {
    # 1. Check Product Count
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/public/products" -Method Get
    # API returns { data: { content: [...] } } or { content: [...] } depending on wrapper
    $products = if ($response.content) { $response.content } elseif ($response.data.content) { $response.data.content } else { $response }
    $count = $products.Count
    
    if ($count -ge 20) {
        Write-Host "✅ SUCCESS: Found $count products in database." -ForegroundColor Green
    } else {
        Write-Host "❌ FAILED: Expected at least 20 products, found $count." -ForegroundColor Red
        if ($count -eq 0) {
            Write-Host "⚠️ Tip: Ensure backend restarted and seed script ran." -ForegroundColor Yellow
        }
    }

    # 2. Check Specific Product (Sherwani)
    $sherwani = $products | Where-Object { $_.itemCode -eq 'VA-SRW-006' }
    if ($sherwani) {
        Write-Host "✅ Verified: 'Velvet Royal Sherwani' exists." -ForegroundColor Green
        Write-Host "   Price: $($sherwani.price)"
        # Note: DTO might not have category returned directly unless added, verify response
    } else {
        Write-Host "❌ FAILED: Specific product 'VA-SRW-006' not found." -ForegroundColor Red
        Write-Host "   Available Items: $($products.itemCode -join ', ')" -ForegroundColor Yellow
    }

} catch {
    Write-Host "❌ ERROR Connecting to Backend: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   Make sure backend is running on port 8080" -ForegroundColor Yellow
}
