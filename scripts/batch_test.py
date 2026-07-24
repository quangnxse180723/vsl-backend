import os
import sys
import time
import string
import random
import urllib.request
import urllib.parse
import json
import uuid

# Fix Windows console unicode issues
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

VIDEO_DIR = r"F:\sba-project\Video_VSL"
API_BASE = "http://localhost:8080/api"

def get_token():
    email = f"test_{''.join(random.choices(string.ascii_lowercase, k=6))}@test.com"
    password = "TestUser@123"
    
    # 1. Register
    reg_data = json.dumps({
        "email": email,
        "password": password,
        "fullName": "Test User",
        "username": email.split('@')[0]
    }).encode('utf-8')
    req = urllib.request.Request(f"{API_BASE}/auth/register", data=reg_data, headers={'Content-Type': 'application/json'})
    urllib.request.urlopen(req)
    
    # 2. Login
    login_data = json.dumps({"email": email, "password": password}).encode('utf-8')
    req = urllib.request.Request(f"{API_BASE}/auth/login", data=login_data, headers={'Content-Type': 'application/json'})
    resp = urllib.request.urlopen(req)
    try:
        res_json = json.loads(resp.read().decode('utf-8'))
        
        # Check if wrapped in data
        if "data" in res_json and "accessToken" in res_json["data"]:
            return res_json["data"]["accessToken"]
        return res_json["accessToken"]
    except Exception as e:
        print("Login failed. Response was:", res_json)
        raise e

def encode_multipart_formdata(fields, files):
    boundary = uuid.uuid4().hex
    buffer = b''
    for key, value in fields.items():
        buffer += f'--{boundary}\r\n'.encode('utf-8')
        buffer += f'Content-Disposition: form-data; name="{key}"\r\n\r\n'.encode('utf-8')
        buffer += f'{value}\r\n'.encode('utf-8')
    for key, (filename, file_content) in files.items():
        buffer += f'--{boundary}\r\n'.encode('utf-8')
        buffer += f'Content-Disposition: form-data; name="{key}"; filename="{filename}"\r\n'.encode('utf-8')
        buffer += f'Content-Type: video/mp4\r\n\r\n'.encode('utf-8')
        buffer += file_content
        buffer += b'\r\n'
    buffer += f'--{boundary}--\r\n'.encode('utf-8')
    return boundary, buffer

def main():
    try:
        token = get_token()
    except Exception as e:
        print("Error getting token:", e)
        return

    headers = {"Authorization": f"Bearer {token}"}
    
    all_files = [f for f in os.listdir(VIDEO_DIR) if f.endswith(".mp4")]
    # Randomize to get a good mix of 20 videos
    random.seed(42)
    random.shuffle(all_files)
    files_to_test = all_files[:20]
    
    print(f"Start testing on {len(files_to_test)} videos (randomly selected)...")
    print("| Video | Expected ID | Result | Confidence | Rank | Latency (s) |")
    print("|---|---|---|---|---|---|")
    
    results = []
    correct_count = 0
    
    for filename in files_to_test:
        filepath = os.path.join(VIDEO_DIR, filename)
        expected_id = filename.split('_')[0].split('.')[0].strip()
        
        start_time = time.time()
        
        try:
            with open(filepath, 'rb') as f:
                file_content = f.read()
                
            boundary, data = encode_multipart_formdata({'expectedId': expected_id}, {'video': (filename, file_content)})
            
            req = urllib.request.Request(f"{API_BASE}/practice/evaluate", data=data)
            req.add_header('Authorization', f'Bearer {token}')
            req.add_header('Content-Type', f'multipart/form-data; boundary={boundary}')
            
            resp = urllib.request.urlopen(req)
            latency = time.time() - start_time
            
            res_json = json.loads(resp.read().decode('utf-8'))
            
            # Unwrap if it's inside 'data'
            ai_result = res_json.get("data", res_json)
            
            status = ai_result.get("status", "UNKNOWN")
            conf = float(ai_result.get("confidence", 0))
            if conf <= 1.0:
                conf *= 100
            rank = ai_result.get("rank", -1)
            
            if status == "CORRECT":
                correct_count += 1
                
            row = f"| {filename} | {expected_id} | {status} | {conf:.2f}% | {rank} | {latency:.2f}s |"
        except urllib.error.HTTPError as e:
            latency = time.time() - start_time
            row = f"| {filename} | {expected_id} | ERROR ({e.code}) | - | - | {latency:.2f}s |"
            print("HTTPError:", e.read().decode('utf-8'))
        except Exception as e:
            latency = time.time() - start_time
            row = f"| {filename} | {expected_id} | ERROR ({str(e)}) | - | - | {latency:.2f}s |"
            
        print(row)
        results.append(row)
        
    print(f"\nTotal test videos: {len(files_to_test)}")
    print(f"Correct Count: {correct_count}/{len(files_to_test)} ({(correct_count/len(files_to_test))*100:.2f}%)")

if __name__ == "__main__":
    main()
