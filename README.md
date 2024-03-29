# PDND Interoperability - Party Management Micro Service
---

### File manager configuration

In order to launch this component you MUST configure a valid file manager using the following parameters:

```
interop-commons {
  storage {
    type = ${STORAGE_TYPE}
    container = ${STORAGE_CONTAINER}  //"interop-onboarding-files"
    endpoint = ${STORAGE_ENDPOINT}
    application {
       id = ${STORAGE_CREDENTIAL_ID}
       secret = ${STORAGE_CREDENTIAL_SECRET}
    }
  }
}
```

#### Parameters

- `storage.type` - defines the chosen file manager (see [below](#admittable-file-managers))
- `storage.container` - defines the file container (e.g.: S3 bucket name, Blob Storage container name)
- `storage.endpoint` - defines the file manager endpoint
- `storage.application.id` - defines the file manager client credential
- `storage.application.secret` - defines the file manager secret credential

#### Admittable file managers

So far, this component offers the following implementations:

- `File` - file manager working on local disk
- `S3` - file manager working with AWS S3
- `BlobStorage` - file manager working with Azure Blob Storage

e.g.: If you like to use S3 file manager, you should configure the storage as follows:

```
interop-commons {
  storage {
    type = "S3"
    //...
  }
}
```