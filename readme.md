# File Storage API Documentation

## Endpoints

### Upload File

`POST /api/files`

Uploads a new file with metadata.

**Parameters:**

- `fileName` (required): Name of the file
- `userId` (required): ID of the user uploading the file
- `visibility` (optional): File visibility (PRIVATE/PUBLIC). Defaults to PRIVATE
- `tags` (optional): Comma-separated list of tags. No more than 5 tags allowed.

**Example:**

```bash
curl -X POST "http://localhost:8080/api/files?fileName={fileName}&userId={userId}&visibility={visibility}&tags={tags}" \
  -F "file=@/path/to/your/file.txt" \
```

**Response:**

```json
{
  "fileName": "myfile.txt",
  "visibility": "PRIVATE",
  "contentType": "text/plain",
  "size": 15,
  "timestamp": "2025-06-24T10:13:02.130Z",
  "url": "http://localhost:8080/files/some-random-uuid"
}
```

---

### Download File

`GET /api/files/{uuid}?userId={userId}`

Downloads a file by its UUID.

**Parameters:**

- `uuid` (required): UUID of the file to download

**Query Parameters:**

- `userId` (optional): User id for access control, required for private files.

**Example:**

```bash
curl -X GET "http://localhost:8080/api/files/{uuid}?userId={userId}" -o downloaded_file.txt
```

---

### List Public Files

`GET /api/files/public`

Retrieves a list of public files, optionally filtered by tag.

**Query Parameters:**

- `page` (optional): Page number for pagination. Defaults to 0.
- `size` (optional): Number of items per page. Defaults to 20.
- `tag` (optional): Tag to filter

**Example:**

```bash
curl -X GET "http://localhost:8080/api/files/public?tag={tag}"
```

**Response:**

```json
{
  "page": 0,
  "size": 20,
  "data": [
    {
      "fileName": "testfile3.txt",
      "contentType": "text/plain",
      "size": "15",
      "visibility": "PUBLIC",
      "tags": [
        "tag1",
        "tag2",
        "tag3"
      ],
      "link": "9b19fbbe-82d0-4b64-b0e1-26be152020f2",
      "uploadDate": "2025-06-24T10:25:55.704Z"
    }
  ]
}
```

---

### List User Files

`GET /api/files/public`

Retrieves a list of public files, optionally filtered by tag.

**Query Parameters:**

- `userId` (required): ID of the user whose files to retrieve
- `page` (optional): Page number for pagination. Defaults to 0.
- `size` (optional): Number of items per page. Defaults to 20.
- `tag` (optional): Tag to filter
- `visibility` (optional): Filter by visibility (PRIVATE/PUBLIC). Defaults to all.

**Example:**

```bash
curl -X GET "http://localhost:8080/api/files?userId={userId}&page={page}&size={size}&tag={tag}&visibility={visibility}"
```

**Response:**

```json
{
  "page": 0,
  "size": 20,
  "data": [
    {
      "fileName": "testfile3.txt",
      "contentType": "text/plain",
      "size": "15",
      "visibility": "PUBLIC",
      "tags": [
        "tag1",
        "tag2",
        "tag3"
      ],
      "link": "9b19fbbe-82d0-4b64-b0e1-26be152020f2",
      "uploadDate": "2025-06-24T10:25:55.704Z"
    }
  ]
}
```

---

### Delete File

`DELETE /api/files/{uuid}`

Deletes a file by its UUID.

**Parameters:**

- `uuid` (required): ID of the file to delete

**Query Parameters:**

- `userId` (required): User id for access control.

**Example:**

```bash
curl -X DELETE "http://localhost:8080/api/files/{uuid}?userId={userId}"
```

**Response:**

HTTP 204 No Content

---

### List Accessible Tags

`GET /api/files/tags`

**Query Parameters:**

- `userId` (required): User id for access control.

**Example:**

```bash
curl -X GET "http://localhost:8080/api/files/tags?userId={userId}"
```

**Response:**

```json
{
  "tags": [
    "tag1",
    "tag2",
    "tag3"
  ]
}
```