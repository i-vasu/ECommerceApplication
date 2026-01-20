import concurrent.futures
import requests
import time
import statistics

URL = "http://localhost:8080/api/products" # Hits DB/Cache
CONCURRENT_REQUESTS = 50
TOTAL_REQUESTS = 1000

def fetch(url):
    start = time.time()
    try:
        resp = requests.get(url, timeout=5)
        latency = (time.time() - start) * 1000
        return resp.status_code, latency
    except Exception as e:
        return 500, 0

def load_test():
    print(f"Starting Load Test: {TOTAL_REQUESTS} requests, {CONCURRENT_REQUESTS} concurrency")
    latencies = []
    status_codes = {}
    
    start_time = time.time()
    
    with concurrent.futures.ThreadPoolExecutor(max_workers=CONCURRENT_REQUESTS) as executor:
        futures = [executor.submit(fetch, URL) for _ in range(TOTAL_REQUESTS)]
        
        for future in concurrent.futures.as_completed(futures):
            code, latency = future.result()
            latencies.append(latency)
            status_codes[code] = status_codes.get(code, 0) + 1
            
    total_time = time.time() - start_time
    tps = TOTAL_REQUESTS / total_time
    
    print("\n--- Results ---")
    print(f"Total Time: {total_time:.2f}s")
    print(f"TPS (Transactions Per Second): {tps:.2f}")
    print(f"Avg Latency: {statistics.mean(latencies):.2f}ms")
    print(f"P95 Latency: {statistics.quantiles(latencies, n=20)[18]:.2f}ms") # 95th Percentile
    print(f"Status Codes: {status_codes}")
    print("----------------")

if __name__ == "__main__":
    load_test()
