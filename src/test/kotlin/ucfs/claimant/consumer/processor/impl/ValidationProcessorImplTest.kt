package ucfs.claimant.consumer.processor.impl

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.kotest.assertions.arrow.either.shouldBeLeft
import io.kotest.assertions.arrow.either.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import ucfs.claimant.consumer.domain.SourceRecord
import ucfs.claimant.consumer.domain.ValidationProcessingOutput

class ValidationProcessorImplTest: StringSpec() {

    init {
        "Valid message passes validation." {
            val json = """
            {
               "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
               "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
               "@type" : "V4",
               "message": {
                   "@type": "hello",
                   "_id": {
                       "declarationId": 1
                   },
                   "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                   "collection" : "addresses",
                   "db": "core",
                   "dbObject": "asd",
                   "encryption": {
                       "keyEncryptionKeyId": "cloudhsm:7,14",
                       "initialisationVector": "iv",
                       "encryptedEncryptionKey": "=="
                   }
               },
              "version" : "core-4.release_152.16",
              "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """
            validateRight(json)
        }


        "Valid message with alternate date format passes validation." {

            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "message": {
                    "@type": "hello",
                    "_id": {
                        "declarationId": 1
                    },
                    "_lastModifiedDateTime": "2019-07-04T07:27:35.104",
                    "collection" : "addresses",
                    "db": "core",
                    "dbObject": "asd",
                    "encryption": {
                        "keyEncryptionKeyId": "cloudhsm:7,14",
                        "initialisationVector": "iv",
                        "encryptedEncryptionKey": "=="
                    }
                },
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """
            validateRight(json)
        }

        "Valid message with alternate date format number two passes validation." {

            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "message": {
                    "@type": "hello",
                    "_id": {
                        "declarationId": 1
                    },
                    "_lastModifiedDateTime": "2017-06-19T23:00:10.875Z",
                    "collection" : "addresses",
                    "db": "core",
                    "dbObject": "asd",
                    "encryption": {
                        "keyEncryptionKeyId": "cloudhsm:7,14",
                        "initialisationVector": "iv",
                        "encryptedEncryptionKey": "=="
                    }
                },
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateRight(json)
        }

        "Additional properties allowed." {

            val json = """
            {
                           "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                           "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                           "@type" : "V4",
               "message": {
                   "@type": "hello",
                   "_id": {
                       "declarationId": 1
                   },
                   "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                   "collection" : "addresses",
                   "db": "core",
                   "dbObject": "asd",
                   "encryption": {
                       "keyEncryptionKeyId": "cloudhsm:7,14",
                       "initialisationVector": "iv",
                       "encryptedEncryptionKey": "==",
                       "additional": [0, 1, 2, 3, 4]
                   },
                   "additional": [0, 1, 2, 3, 4]
               },
               "additional": [0, 1, 2, 3, 4],
               "version" : "core-4.release_152.16",
               "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateRight(json)
        }

        "Missing '#/message' causes validation failure." {
            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "msg": [0, 1, 2],
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateLeft(json)
        }

        "Incorrect '#/message' type causes validation failure." {
            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "message": 123,
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateLeft(json)
        }

        "Missing '#/message/@type' field causes validation failure." {

            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "message": {
                    "_id": {
                        "declarationId": 1
                    },
                    "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                    "collection" : "addresses",
                    "db": "core",        "dbObject": "asd",        "encryption": {
                        "keyEncryptionKeyId": "cloudhsm:7,14",
                        "initialisationVector": "iv",
                        "encryptedEncryptionKey": "=="
                    }
                },
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateLeft(json)
        }

        "Incorrect '#/message/@type' type causes validation failure." {

            val json = """
                {
                   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                   "@type" : "V4",
                   "message": {
                       "_id": {
                           "declarationId": 1
                       },
                       "@type": 1,
                       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                       "collection" : "addresses",
                       "db": "core",
                       "dbObject": "asd",
                       "encryption": {
                           "keyEncryptionKeyId": "cloudhsm:7,14",
                           "initialisationVector": "iv",
                           "encryptedEncryptionKey": "=="
                       }
                   },
                  "version" : "core-4.release_152.16",
                  "timestamp" : "2020-08-05T07:07:00.105+0000"
                }
                """

            validateLeft(json)
        }

        "String '#/message/_id' field does not cause validation failure." {

            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "message": {
                    "@type": "hello",
                    "_id": "abcdefg",
                    "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                    "db": "abcd",
                    "collection" : "addresses",
                    "dbObject": "asd",
                    "encryption": {
                        "keyEncryptionKeyId": "cloudhsm:7,14",
                        "initialisationVector": "iv",
                        "encryptedEncryptionKey": "=="
                    }
                },
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateRight(json)
        }

        "Integer '#/message/_id' field does not cause validation failure." {

            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "message": {
                    "@type": "hello",
                    "_id": 12345,
                    "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                    "db": "abcd",
                    "collection" : "addresses",
                    "dbObject": "asd",
                    "encryption": {
                        "keyEncryptionKeyId": "cloudhsm:7,14",
                        "initialisationVector": "iv",
                        "encryptedEncryptionKey": "=="
                    }
                },
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateRight(json)
        }

        "Empty string '#/message/_id' field causes validation failure." {
            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "message": {
                    "@type": "hello",
                    "_id": "",
                    "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                    "db": "abcd",
                    "collection" : "addresses",
                    "dbObject": "asd",
                    "encryption": {
                        "keyEncryptionKeyId": "cloudhsm:7,14",
                        "initialisationVector": "iv",
                        "encryptedEncryptionKey": "=="
                    }
                },
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateLeft(json)
        }

        "Incorrect '#/message/_id' type causes validation failure." {
            val json = """{
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "message": {
                    "@type": "hello",
                    "_id": [1, 2, 3, 4, 5, 6, 7 ,8 , 9],
                    "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                    "db": "abcd",
                    "collection" : "addresses",
                    "dbObject": "asd",
                    "encryption": {
                        "keyEncryptionKeyId": "cloudhsm:7,14",
                        "initialisationVector": "iv",
                        "encryptedEncryptionKey": "=="
                    }
                },
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateLeft(json)
        }

        "Empty '#/message/_id' type causes validation failure." {

            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "message": {
                    "@type": "hello",
                    "_id": {},
                    "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                    "db": "abcd",
                    "collection" : "addresses",
                    "dbObject": "asd",
                    "encryption": {
                        "keyEncryptionKeyId": "cloudhsm:7,14",
                        "initialisationVector": "iv",
                        "encryptedEncryptionKey": "=="
                    }
                },
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateLeft(json)
        }

        "Missing '#/message/_lastModifiedDateTime' does not cause validation failure." {

            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "message": {
                    "@type": "hello",
                    "_id": { part: 1},
                    "db": "abcd",
                    "collection" : "addresses",
                    "dbObject": "asd",
                    "encryption": {
                        "keyEncryptionKeyId": "cloudhsm:7,14",
                        "initialisationVector": "iv",
                        "encryptedEncryptionKey": "=="
                    }
                },
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateRight(json)
        }

        "Null '#/message/_lastModifiedDateTime' does not cause validation failure." {

            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "message": {
                    "@type": "hello",
                    "_id": {
                        "declarationId": 1
                    },
                    "_lastModifiedDateTime": null,
                    "collection" : "addresses",
                    "db": "core",
                    "dbObject": "asd",
                    "encryption": {
                        "keyEncryptionKeyId": "cloudhsm:7,14",
                        "initialisationVector": "iv",
                        "encryptedEncryptionKey": "=="
                    }
                },
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateRight(json)
        }

        "Empty '#/message/_lastModifiedDateTime' does not cause validation failure." {

            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "message": {
                    "@type": "hello",
                    "_id": {
                        "declarationId": 1
                    },
                    "_lastModifiedDateTime": "",
                    "collection" : "addresses",
                    "db": "core",
                    "dbObject": "asd",
                    "encryption": {
                        "keyEncryptionKeyId": "cloudhsm:7,14",
                        "initialisationVector": "iv",
                        "encryptedEncryptionKey": "=="
                    }
                },
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateRight(json)
        }


        "Incorrect '#/message/_lastModifiedDateTime' type causes validation failure." {

            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "message": {
                    "@type": "hello",
                    "_id": { part: 1},
                    "_lastModifiedDateTime": 12,
                    "db": "abcd",
                    "collection" : "addresses",
                    "dbObject": "asd",
                    "encryption": {
                        "keyEncryptionKeyId": "cloudhsm:7,14",
                        "initialisationVector": "iv",
                        "encryptedEncryptionKey": "=="
                    }
                },
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateLeft(json)
        }

        "Incorrect '#/message/_lastModifiedDateTime' format causes validation failure." {

            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "message": {
                    "@type": "hello",
                    "_id": { part: 1},
                    "_lastModifiedDateTime": "2019-07-04",
                    "db": "abcd",
                    "collection" : "addresses",
                    "dbObject": "asd",
                    "encryption": {
                        "keyEncryptionKeyId": "cloudhsm:7,14",
                        "initialisationVector": "iv",
                        "encryptedEncryptionKey": "=="
                    }
                },
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateLeft(json)
        }

        "Missing '#/message/db' field causes validation failure." {

            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "message": {
                    "@type": "hello",
                    "_id": {
                        "declarationId": 1
                    },
                    "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                    "collection" : "addresses",
                    "dbObject": "asd",
                    "encryption": {
                        "keyEncryptionKeyId": "cloudhsm:7,14",
                        "initialisationVector": "iv",
                        "encryptedEncryptionKey": "=="
                    }
                },
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateLeft(json)
        }

        "Incorrect '#/message/db' type  causes validation failure." {

            val json = """
                {
                   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                   "@type" : "V4",
                   "message": {
                       "@type": "hello",
                       "_id": {
                           "declarationId": 1
                       },
                       "db": [0, 1, 2],
                       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                       "collection" : "addresses",
                       "dbObject": "asd",
                       "encryption": {
                           "keyEncryptionKeyId": "cloudhsm:7,14",
                           "initialisationVector": "iv",
                           "encryptedEncryptionKey": "=="
                       }
                   },
                  "version" : "core-4.release_152.16",
                  "timestamp" : "2020-08-05T07:07:00.105+0000"
                }
                """

            validateLeft(json)
        }

        "Empty '#/message/db' causes validation failure." {

            val json = """
                {
                   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                   "@type" : "V4",
                   "message": {
                       "@type": "hello",
                       "_id": {
                           "declarationId": 1
                       },
                       "db": "",
                       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                       "collection" : "addresses",
                       "dbObject": "asd",
                       "encryption": {
                           "keyEncryptionKeyId": "cloudhsm:7,14",
                           "initialisationVector": "iv",
                           "encryptedEncryptionKey": "=="
                       }
                   },
                  "version" : "core-4.release_152.16",
                  "timestamp" : "2020-08-05T07:07:00.105+0000"
                }
                """

            validateLeft(json)
        }

        "Missing '#/message/collection' field causes validation failure." {

            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "message": {
                    "@type": "hello",
                    "_id": {
                        "declarationId": 1
                    },
                    "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                    "db" : "addresses",
                    "dbObject": "asd",
                    "encryption": {
                        "keyEncryptionKeyId": "cloudhsm:7,14",
                        "initialisationVector": "iv",
                        "encryptedEncryptionKey": "=="
                    }
                },
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateLeft(json)
        }

        "Incorrect '#/message/collection' type  causes validation failure." {

            val json = """
                {
                   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                   "@type" : "V4",
                   "message": {
                       "@type": "hello",
                       "_id": {
                           "declarationId": 1
                       },
                       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                       "db" : "addresses",
                       "collection": 5,
                       "dbObject": "asd",
                       "encryption": {
                           "keyEncryptionKeyId": "cloudhsm:7,14",
                           "initialisationVector": "iv",
                           "encryptedEncryptionKey": "=="
                       }
                   },
                  "version" : "core-4.release_152.16",
                  "timestamp" : "2020-08-05T07:07:00.105+0000"
                }
                """

            validateLeft(json)
        }

        "Empty '#/message/collection' causes validation failure." {

            val json = """
                {
                   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                   "@type" : "V4",
                   "message": {
                       "@type": "hello",
                       "_id": {
                           "declarationId": 1
                       },
                       "db": "address",
                       "collection": "",
                       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                       "dbObject": "asd",
                       "encryption": {
                           "keyEncryptionKeyId": "cloudhsm:7,14",
                           "initialisationVector": "iv",
                           "encryptedEncryptionKey": "=="
                       }
                   },
                  "version" : "core-4.release_152.16",
                  "timestamp" : "2020-08-05T07:07:00.105+0000"
                }
                """

            validateLeft(json)
        }


        "Missing '#/message/dbObject' field causes validation failure." {

            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "message": {
                    "@type": "hello",
                    "_id": {
                        "declarationId": 1
                    },
                    "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                    "db" : "addresses",
                    "collection": "core",
                    "encryption": {
                        "keyEncryptionKeyId": "cloudhsm:7,14",
                        "initialisationVector": "iv",
                        "encryptedEncryptionKey": "=="
                    }
                },
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateLeft(json)
        }

        "Incorrect '#/message/dbObject' type  causes validation failure." {

            val json = """
                {
                   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                   "@type" : "V4",
                   "message": {
                       "@type": "hello",
                       "_id": {
                           "declarationId": 1
                       },
                       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                       "db" : "addresses",
                       "collection": "collection",
                       "dbObject": { "key": "value" },
                       "encryption": {
                           "keyEncryptionKeyId": "cloudhsm:7,14",
                           "initialisationVector": "iv",
                           "encryptedEncryptionKey": "=="
                       }
                   },
                  "version" : "core-4.release_152.16",
                  "timestamp" : "2020-08-05T07:07:00.105+0000"
                }
                """

            validateLeft(json)
        }

        "Empty '#/message/dbObject' causes validation failure." {

            val json = """
                {
                   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                   "@type" : "V4",
                   "message": {
                       "@type": "hello",
                       "_id": {
                           "declarationId": 1
                       },
                       "db": "address",
                       "collection": "collection",
                       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                       "dbObject": "",
                       "encryption": {
                           "keyEncryptionKeyId": "cloudhsm:7,14",
                           "initialisationVector": "iv",
                           "encryptedEncryptionKey": "=="
                       }
                   },
                  "version" : "core-4.release_152.16",
                  "timestamp" : "2020-08-05T07:07:00.105+0000"
                }
                """

            validateLeft(json)
        }

        "Missing '#/message/encryption' causes validation failure." {

            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "message": {
                    "@type": "hello",
                    "_id": {
                        "declarationId": 1
                    },
                    "db": "address",
                    "collection": "collection",
                    "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                    "dbObject": "123"
                },
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateLeft(json)
        }

        "Incorrect '#/message/encryption' type causes validation failure." {

            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "message": {
                    "@type": "hello",
                    "_id": {
                        "declarationId": 1
                    },
                    "db": "address",
                    "collection": "collection",
                    "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                    "dbObject": "123",
                    "encryption": "hello"
                },
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateLeft(json)
        }

        "Missing keyEncryptionKeyId from '#/message/encryption' type causes validation failure." {

            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "message": {
                    "@type": "hello",
                    "_id": {
                        "declarationId": 1
                    },
                    "db": "address",
                    "collection": "collection",
                    "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                    "dbObject": "123",
                    "encryption": {
                        "initialisationVector": "iv",
                        "encryptedEncryptionKey": "=="
                    }
                },
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateLeft(json)
        }

        "Missing initialisationVector from '#/message/encryption' type causes validation failure." {

            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "message": {
                    "@type": "hello",
                    "_id": {
                        "declarationId": 1
                    },
                    "db": "address",
                    "collection": "collection",
                    "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                    "dbObject": "123",
                    "encryption": {
                        "keyEncryptionKeyId": "cloudhsm:1,2",
                        "encryptedEncryptionKey": "=="
                    }
                },
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateLeft(json)
        }

        "Missing encryptedEncryptionKey from '#/message/encryption' type causes validation failure." {

            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                "@type" : "V4",
                "message": {
                    "@type": "hello",
                    "_id": {
                        "declarationId": 1
                    },
                    "db": "address",
                    "collection": "collection",
                    "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                    "dbObject": "123",
                    "encryption": {
                        "keyEncryptionKeyId": "cloudhsm:7,14",
                        "initialisationVector": "iv"
                    }
                },
                "version" : "core-4.release_152.16",
                "timestamp" : "2020-08-05T07:07:00.105+0000"
            }
            """

            validateLeft(json)
        }

        "Empty keyEncryptionKeyId from '#/message/encryption' type causes validation failure." {

            val json = """
                {
                   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                   "@type" : "V4",
                   "message": {
                       "@type": "hello",
                       "_id": {
                           "declarationId": 1
                       },
                       "db": "address",
                       "collection": "collection",
                       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                       "dbObject": "123",
                       "encryption": {
                           "keyEncryptionKeyId": "",
                           "initialisationVector": "iv",
                           "encryptedEncryptionKey": "=="
                       }
                   },
                  "version" : "core-4.release_152.16",
                  "timestamp" : "2020-08-05T07:07:00.105+0000"
                }
                """

            validateLeft(json)
        }

        "Empty initialisationVector from '#/message/encryption' type causes validation failure." {

            val json = """
                {
                   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                   "@type" : "V4",
                   "message": {
                       "@type": "hello",
                       "_id": {
                           "declarationId": 1
                       },
                       "db": "address",
                       "collection": "collection",
                       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                       "dbObject": "123",
                       "encryption": {
                           "keyEncryptionKeyId": "cloudhsm:1,2",
                           "initialisationVector": "",
                           "encryptedEncryptionKey": "=="
                       }
                   },
                  "version" : "core-4.release_152.16",
                  "timestamp" : "2020-08-05T07:07:00.105+0000"
                }
                """

            validateLeft(json)
        }

        "Empty encryptedEncryptionKey from '#/message/encryption' type causes validation failure." {

            val json = """
                {
                   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                   "@type" : "V4",
                   "message": {
                       "@type": "hello",
                       "_id": {
                           "declarationId": 1
                       },
                       "db": "address",
                       "collection": "collection",
                       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                       "dbObject": "123",
                       "encryption": {
                           "keyEncryptionKeyId": "cloudhsm:7,14",
                           "initialisationVector": "iv",
                           "encryptedEncryptionKey": ""
                       }
                   },
                  "version" : "core-4.release_152.16",
                  "timestamp" : "2020-08-05T07:07:00.105+0000"
                }
                """

            validateLeft(json)
        }

        "Incorrect '#/message/encryption/keyEncryptionKeyId' type causes validation failure." {

            val json = """
                {
                   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                   "@type" : "V4",
                   "message": {
                       "@type": "hello",
                       "_id": {
                           "declarationId": 1
                       },
                       "db": "address",
                       "collection": "collection",
                       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                       "dbObject": "123",
                       "encryption": {
                           "keyEncryptionKeyId": 0,
                           "initialisationVector": "iv",
                           "encryptedEncryptionKey": "=="
                       }
                   },
                  "version" : "core-4.release_152.16",
                  "timestamp" : "2020-08-05T07:07:00.105+0000"
                }
                """

            validateLeft(json)
        }

        "Incorrect initialisationVector '#/message/encryption/initialisationVector' type causes validation failure." {

            val json = """
                {
                   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                   "@type" : "V4",
                   "message": {
                       "@type": "hello",
                       "_id": {
                           "declarationId": 1
                       },
                       "db": "address",
                       "collection": "collection",
                       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                       "dbObject": "123",
                       "encryption": {
                           "keyEncryptionKeyId": "cloudhsm:1,2",
                           "initialisationVector": {},
                           "encryptedEncryptionKey": "=="
                       }
                  },
                  "version" : "core-4.release_152.16",
                  "timestamp" : "2020-08-05T07:07:00.105+0000"
                }
                """

            validateLeft(json)
        }

        "Incorrect '#/message/encryption/encryptedEncryptionKey' type causes validation failure." {

            val json = """
                {
                   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                   "@type" : "V4",
                   "message": {
                       "@type": "hello",
                       "_id": {
                           "declarationId": 1
                       },
                       "db": "address",
                       "collection": "collection",
                       "_lastModifiedDateTime": "2019-07-04T07:27:35.104+0000",
                       "dbObject": "123",
                       "encryption": {
                           "keyEncryptionKeyId": "cloudhsm:7,14",
                           "initialisationVector": "iv",
                           "encryptedEncryptionKey": [0, 1, 2]
                       }
                  },
                  "version" : "core-4.release_152.16",
                  "timestamp" : "2020-08-05T07:07:00.105+0000"
                }
                """

            validateLeft(json)
        }

        "Incorrect keyEncryptionKeyId '#/message/encryption/keyEncryptionKeyId' type causes validation failure." {

            val json = """
                {
                   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                   "@type" : "V4",
                   "message" : {
                       "dbObject" : "xxxxxx",
                       "encryption" : {
                           "keyEncryptionKeyId" : "cloudhsm:aaa,bbbb",
                           "encryptedEncryptionKey" : "xxxxxx",
                           "initialisationVector" : "xxxxxxxx=="
                       },
                       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                       "db": "address",
                       "collection": "collection",
                       "_id" : {
                           "anyId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                       }
                   },
                   "version" : "core-4.release_147.3",
                   "timestamp" : "2020-05-21T17:18:15.706+0000"
                }
                """

            validateLeft(json)
        }

        "'#/message/unitOfWorkId' is required" {

            val json = """
                {
                   "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                   "@type" : "V4",
                   "message" : {
                       "dbObject" : "xxxxxx",
                       "encryption" : {
                           "keyEncryptionKeyId" : "cloudhsm:7,14",
                           "encryptedEncryptionKey" : "xxxxxx",
                           "initialisationVector" : "xxxxxxxx=="
                       },
                       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                       "db": "address",
                       "collection": "collection",
                       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                       "_id" : {
                           "anyId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                       }
                   },
                   "version" : "core-4.release_147.3",
                   "timestamp" : "2020-05-21T17:18:15.706+0000"
                }
                """

            validateLeft(json)
        }

        "'#/message/unitOfWorkId' can be null" {

            val json = """
            {
                "traceId" : "091f29ab-b6c5-411c-851e-15683ce53c40",
                "unitOfWorkId" : null,
                "@type" : "V4",
                "message" : {
                    "dbObject" : "xxxxxx",
                    "encryption" : {
                        "keyEncryptionKeyId" : "cloudhsm:7,14",
                        "encryptedEncryptionKey" : "xxxxxx",
                        "initialisationVector" : "xxxxxxxx=="
                    },
                    "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                    "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                    "db": "address",
                    "collection": "collection",
                    "_id" : {
                        "anyId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                    }
                },
                "version" : "core-4.release_147.3",
                "timestamp" : "2020-05-21T17:18:15.706+0000"
            }
            """

            validateRight(json)
        }

        "'#/message/traceId' is required" {

            val json = """
                    {
                       "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                       "@type" : "V4",
                       "message" : {
                           "dbObject" : "xxxxxx",
                           "encryption" : {
                               "keyEncryptionKeyId" : "cloudhsm:7,14",
                               "encryptedEncryptionKey" : "xxxxxx",
                               "initialisationVector" : "xxxxxxxx=="
                           },
                           "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                           "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                           "db": "address",
                           "collection": "collection",
                           "_id" : {
                               "anyId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                           }
                       },
                       "version" : "core-4.release_147.3",
                       "timestamp" : "2020-05-21T17:18:15.706+0000"
                    }
                    """

            validateLeft(json)
        }

        "'#/message/traceId' can be null" {

            val json = """
                {
                   "traceId" : null,
                   "unitOfWorkId" : "31faa55f-c5e8-4581-8973-383db31ddd77",
                   "@type" : "V4",
                   "message" : {
                       "dbObject" : "xxxxxx",
                       "encryption" : {
                           "keyEncryptionKeyId" : "cloudhsm:7,14",
                           "encryptedEncryptionKey" : "xxxxxx",
                           "initialisationVector" : "xxxxxxxx=="
                       },
                       "@type" : "EQUALITY_QUESTIONS_ANSWERED",
                       "_lastModifiedDateTime" : "2020-05-21T17:18:15.706+0000",
                       "db": "address",
                       "collection": "collection",
                       "_id" : {
                           "anyId" : "f1d4723b-fdaa-4123-8e20-e6eca6c03645"
                       }
                   },
                   "version" : "core-4.release_147.3",
                   "timestamp" : "2020-05-21T17:18:15.706+0000"
                }
                """

            validateRight(json)
        }
    }

    private fun validateRight(json: String) {
        validationOutput(json) shouldBeRight Pair(sourceRecord, json)
    }

    private fun validateLeft(json: String) {
        validationOutput(json) shouldBeLeft sourceRecord
    }

    private fun validationOutput(json: String): ValidationProcessingOutput =
            ValidationProcessorImpl(schemaLocation).process(Pair(sourceRecord, json))

    private val schemaLocation: String = "/message.schema.json"
    private val sourceRecord = mock<SourceRecord> {
        on { topic() } doReturn "db.database.collection"
        on { offset() } doReturn 1
        on { partition() } doReturn 0
        on { key() } doReturn "key".toByteArray()
        on { timestamp() } doReturn 100L
    }
}
