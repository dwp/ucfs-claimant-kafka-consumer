# Transformation requirements

## Context

We currently do the transformations in Crown on a full batch of data.
The [existing transformation application](https://git.ucd.gpn.gov.uk/dip/ucfs-claimant-etl)
is written in Go Lang. The intention is to reuse the logic, but write it in the
new Kotlin app (kotlin allows us to reuse K2HBs consumer code)
Database contents look like this per table:

### Claimant

    {
        data: {
            "_id": {
                "citizenId": "2bee0d32-4e18-477c-b5b1-b46d7952a927"
            },
            "nino": "6V1iuWfWfx8Uft0UpSCYddAnmFO4Y0Ndr3eyhrlIvLZ2bThx6YLQqsdzv_S_A9VjwuD7U_U-ItmPvnOdRPoG4w=="
        },
        nino: 6V1iuWfWfx8Uft0UpSCYddAnmFO4Y0Ndr3eyhrlIvLZ2bThx6YLQqsdzv_S_A9VjwuD7U_U-ItmPvnOdRPoG4w==,
        citizen_id: 2bee0d32-4e18-477c-b5b1-b46d7952a927
    }

### Contract

    {
        data: {
            "_id": {
                "contractId": "4f24a036-53b6-44c0-a245-4ed031359ea2"
            },
            "people": ["36804443-089c-4d32-b071-75945c216082"],
            "startDate": 20200430,
            "closedDate": null,
            "contractType": "INITIAL",
            "declaredDate": 20200430,
            "coupleContract": false,
            "claimSuspension": {"suspensionDate": null},
            "createdDateTime": {"$date": "2020-04-30T01:10:11.928839Z"},
            "entitlementDate": 20200507
        },
        contract_id: 4f24a036-53b6-44c0-a245-4ed031359ea2,
        citizen_a: 36804443-089c-4d32-b071-75945c216082,
        citizen_b: NULL
    }

OR for a couple's contract:

    {
        data: {
            "_id": {
                "contractId": "b6554fbb-a207-4afb-b38e-62d9a54afc85"
            },
            "people": [
                "ab4863f1-836a-4233-9357-7aef636451a7",
                "fb36cc6c-19e9-4015-ad15-3761e7db0d52"
            ],
            "startDate": 20200430,
            "closedDate": null,
            "contractType": "INITIAL",
            "declaredDate": 20200430,
            "coupleContract": false,
            "claimSuspension": {"suspensionDate": null},
            "createdDateTime": {"$date": "2020-04-30T01:29:01.935224Z"},
            "entitlementDate": 20200507
        },
        contract_id: b6554fbb-a207-4afb-b38e-62d9a54afc85,
        citizen_a: ab4863f1-836a-4233-9357-7aef636451a7,
        citizen_b: fb36cc6c-19e9-4015-ad15-3761e7db0d52
    }

### Statement

    {
        data: {
            "_id": {
                "statementId": "02b8d8d2-e62a-49bf-9115-8500c5aa8e13"
            },
            "people": [
                {
                    "citizenId": "53bb5c90-8d50-455e-a782-d3d118cce4da",
                    "contractId": "6e2b4428-2da5-4f77-9904-e0c2fc850c4f"
                }
            ],
            "takeHomePay": "ENCRYPTED",
            "createdDateTime": {"$date": "2027-12-01T00:00:00.000000Z"},
            "assessmentPeriod": {
                "endDate": 20280131,
                "startDate": 20280101,
                "contractId": "6e2b4428-2da5-4f77-9904-e0c2fc850c4f",
                "paymentDate": 20280123,
                "processDate": null,
                "createdDateTime": {"$date": "2027-12-01T00:00:00.000000Z"},
                "assessmentPeriodId": "5395ec9d-d054-4bf4-b027-cc11d1ec473d"
            },
            "encryptedTakeHomePay": {
                "keyId": "arn:aws:kms:eu-west-1:999999999:key/08db5e60-156c-4e41-b61f-60a3556efd7e",
                "takeHomePay": "RYiHtPZQAg_lS7zp2wqJvdY7YAY44dzo14LAY47Djfct6w==",
                "cipherTextBlob": "AQIDAHgQyXAXxSvKZWr5lmknNGdf6xcDAe9LpDG9V2tYEZy0uAFRn1I0MWXO-e48E10rijJKAAAAfjB8BgkqhkiG9w0BBwagbzBtAgEAMGgGCSqGSIb3DQEHATAeBglghkgBZQMEAS4wEQQMyegZMclqXE6YwkqCAgEQgDuZPA495mRkI87w3Sx4moCjfkFGv3V6SMI4A5a9yDbueoW88l0qoXHcc-WFmlqhWzEplJHymzbRN0Ymag=="
            }
        },
        contract_id: 6e2b4428-2da5-4f77-9904-e0c2fc850c4f
    }

## Acceptance Criteria

* NINO appended with a salt and hashed
* Take home pay is encrypted using KMS CMK
* Transformed data needs to be in the following format per topic

### Claimant

    {
        "_id": {
            "citizenId": "2bee0d32-4e18-477c-b5b1-b46d7952a927"
        },
        "nino": "6V1iuWfWfx8Uft0UpSCYddAnmFO4Y0Ndr3eyhrlIvLZ2bThx6YLQqsdzv_S_A9VjwuD7U_U-ItmPvnOdRPoG4w=="
    }


### Contract

    {
        "_id": {
            "contractId": "4f24a036-53b6-44c0-a245-4ed031359ea2"
        },
        "people": ["36804443-089c-4d32-b071-75945c216082"],
        "startDate": 20200430,
        "closedDate": null,
        "contractType": "INITIAL",
        "declaredDate": 20200430,
        "coupleContract": false,
        "claimSuspension": {"suspensionDate": null},
        "createdDateTime": {"$date": "2020-04-30T01:10:11.928839Z"},
        "entitlementDate": 20200507
    }

OR for a couple's contract

    {
        "_id": {
            "contractId": "b6554fbb-a207-4afb-b38e-62d9a54afc85"
        },
        "people": [
            "ab4863f1-836a-4233-9357-7aef636451a7",
            "fb36cc6c-19e9-4015-ad15-3761e7db0d52"
        ],
        "startDate": 20200430,
        "closedDate": null,
        "contractType": "INITIAL",
        "declaredDate": 20200430,
        "coupleContract": false,
        "claimSuspension": {
            "suspensionDate": null
        },
        "createdDateTime": {"$date": "2020-04-30T01:29:01.935224Z"},
        "entitlementDate": 20200507
    }

### Statement

    {
        "_id": {
            "statementId": "02b8d8d2-e62a-49bf-9115-8500c5aa8e13"
        },
        "people": [
            {
                "citizenId": "53bb5c90-8d50-455e-a782-d3d118cce4da",
                "contractId": "6e2b4428-2da5-4f77-9904-e0c2fc850c4f"
            }
        ],
        "takeHomePay": "ENCRYPTED",
        "createdDateTime": {"$date": "2027-12-01T00:00:00.000000Z"},
        "assessmentPeriod": {
            "endDate": 20280131,
            "startDate": 20280101,
            "contractId": "6e2b4428-2da5-4f77-9904-e0c2fc850c4f",
            "paymentDate": 20280123,
            "processDate": null,
            "createdDateTime": {"$date": "2027-12-01T00:00:00.000000Z"},
            "assessmentPeriodId": "5395ec9d-d054-4bf4-b027-cc11d1ec473d"
        },
        "encryptedTakeHomePay": {
            "keyId": "arn:aws:kms:eu-west-1:000000000000000:key/08db5e60-156c-4e41-b61f-60a3556efd7e",
            "takeHomePay": "RYiHtPZQAg_lS7zp2wqJvdY7YAY44dzo14LAY47Djfct6w==",
            "cipherTextBlob": "AQIDAHgQyXAXxSvKZWr5lmknNGdf6xcDAe9LpDG9V2tYEZy0uAFRn1I0MWXO-e48E10rijJKAAAAfjB8BgkqhkiG9w0BBwagbzBtAgEAMGgGCSqGSIb3DQEHATAeBglghkgBZQMEAS4wEQQMyegZMclqXE6YwkqCAgEQgDuZPA495mRkI87w3Sx4moCjfkFGv3V6SMI4A5a9yDbueoW88l0qoXHcc-WFmlqhWzEplJHymzbRN0Ymag=="
        }
    }


## Implementation

The three unencrypted dbObjects will look like this:

* [Claimant](https://github.ucds.io/dip/ucfs-claimant-test-data/blob/a2fb483a27db052eeab2c17d38a43df23f49a2dc/generate_test_data.py#L76...L122)


        {
            "_id": {
                "citizenId": citizen_id
            },
            "personId": person_id,
            "firstName": f"Joe{unique_suffix}",
            "lastName": f"Bloggs{unique_suffix}",
            "nino": nino,
            "claimantProvidedNino": None,
            "createdDateTime": {
                "$date": month_delta(datetime.today(), -3).strftime(time_stamp_format)
            },
            "_version": 29,
            "hasVerifiedEmailId": True,
            "inactiveAccountEmailReminderSentDate": None,
            "managedJourney": {
                "type": "REG_SINGLE",
                "currentStep": "COMPLETE"
            },
            "anonymous": False,
            "govUkVerifyStatus": "VERIFY_NOT_ATTEMPTED",
            "cisInterestStatus": "NOT_SET",
            "deliveryUnits": [
                "76783bd7-f709-46bf-8ea4-b0379e7ae1d3"
            ],
            "workCoach": {
                "type": "WORK_COACH_NOT_REQUIRED"
            },
            "supportingAgents": [],
            "currentWorkGroupOnCurrentContract": "INTENSIVE",
            "caseManager": None,
            "claimantDeathDetails": None,
            "pilotGroup": None,
            "firstNameCaseInsensitive": "jdata",
            "lastNameCaseInsensitive": "works",
            "_entityVersion": 0,
            "_lastModifiedDateTime": {
                "$date": month_delta(datetime.today(), -1).strftime(time_stamp_format)
            },
            "identityDocumentsStatus": None,
            "bookAppointmentStatus": "ALL_BOOKED",
            "webAnalyticsUserId": None,
            "searchableNames": [
                "data",
                "works"
            ]
        }

* [Contract](https://github.ucds.io/dip/ucfs-claimant-test-data/blob/a2fb483a27db052eeab2c17d38a43df23f49a2dc/generate_test_data.py#L134...L178)

        {
            "_id": {
                "contractId": contract_id
            },
            "assessmentPeriods": [],
            "people": people,
            "declaredDate": int((month_delta(datetime.today(), -2) - timedelta(days=7)).strftime("%Y%m%d")),
            "startDate": int((month_delta(datetime.today(), -2) - timedelta(days=7)).strftime("%Y%m%d")),
            "entitlementDate": int(month_delta(datetime.today(), -2).strftime("%Y%m%d")),
            "closedDate": int(item['contract_closed_date']) if "contract_closed_date" in item else None,
            "annualVerificationEligibilityDate": None,
            "annualVerificationCompletionDate": None,
            "paymentDayOfMonth": payment_day_of_month,
            "flags": [],
            "claimClosureReason": "FraudIntervention",
            "_version": 12,
            "createdDateTime": {
                "$date": (month_delta(datetime.today(), -2) - timedelta(days=7)).strftime(time_stamp_format)
            },
            "coupleContract": False,
            "claimantsExemptFromWaitingDays": [],
            "contractTypes": None,
            "_entityVersion": 2,
            "_lastModifiedDateTime": {
                "$date": month_delta(datetime.today(), -1).strftime(time_stamp_format)
            },
            "stillSingle": True,
            "contractType": "INITIAL"
        }

* [Statement](https://github.ucds.io/dip/ucfs-claimant-test-data/blob/a2fb483a27db052eeab2c17d38a43df23f49a2dc/generate_test_data.py#L248...L299)

        {
            "_id": {
                "statementId": statement_id
            },
            "_version": 1,
            "people": people_data,
            "assessmentPeriod": assessment_period,
            "standardAllowanceElement": "317.82",
            "housingElement": "0.00",
            "housingElementRent": "0.00",
            "housingElementServiceCharges": "0.00",
            "childElement": "0.00",
            "numberEligibleChildren": 0,
            "disabledChildElement": "0.00",
            "numberEligibleDisabledChildren": 0,
            "childcareElement": "0.00",
            "numberEligibleChildrenInChildCare": 0,
            "carerElement": "0.00",
            "numberPeopleCaredFor": 0,
            "takeHomePay": item['amount'],
            "takeHomeBreakdown": {
                "rte": "0.00",
                "selfReported": "0.00",
                "selfEmployed": "0.00",
                "selfEmployedWithMif": "0.00"
            },
            "unaffectedPayElement": "0.00",
            "totalReducedForHomePay": "0.00",
            "otherIncomeAdjustment": "0.00",
            "capitalAdjustment": "0.00",
            "totalAdjustments": "0.00",
            "fraudPenalties": "0.00",
            "sanctions": "317.82",
            "advances": "0.00",
            "deductions": "0.00",
            "totalPayment": "0.00",
            "createdDateTime": assessment_period['createdDateTime'],
            "earningsSource": None,
            "otherBenefitAwards": [],
            "overlappingBenefits": [],
            "totalOtherBenefitsAdjustment": "0",
            "capApplied": None,
            "type": "CALCULATED",
            "preAdjustmentTotal": "317.82",
            "_entityVersion": 4,
            "_lastModifiedDateTime": assessment_period['createdDateTime'],
            "workCapabilityElement": None,
            "benefitCapThreshold": None,
            "benefitCapAdjustment": None,
            "gracePeriodEndDate": None,
            "landlordPayment": "0"
        }


You can also view the complied output of the above on server {{claimant-api01.node.prd.dw}} in directory {{/srv/export-test/}}
* Map the fields from the source dbObjects to the structures in the AC (field names match)
* You only need to create the {{data}} DB field, the others are autogenerated from the data's contents on insert
* NINO needs salting and hashing
  * salted with value retrieved from {{/ucfs/claimant-api/nino/salt}} parameter in Systems Manager
  * this is created in DW-5178
  * the parameter name should be set in an env var
  * use the word {{SALT}} in local testing
  * hashed using:
    * local command: {{printf '%s%s' "AA123456A" "SALT"|openssl dgst -sha512 -binary|base64|tr +/ -_}}
    * where "AA123456A" is the NINO and the hashed result is:
    {{xFJrf8lbU4G-LB3gx6uF0z531bs0DIVYQ5o5514Y5OrrlxEriQ_W-jEum6bgveIL9gFwwRswDXz8lgqmTQCgFg==}}

    * [existing hashing function|https://git.ucd.gpn.gov.uk/dip/ucfs-claimant-etl/blob/master/pb/hash.go] (written in Go)
* takeHomePay in dbObject needs to be encrypted and new encryptedTakeHomePay object formed to match structure AC
  * KMS encrypted using claimant ETL CMK with alias {{ucfs_etl_cmk}} [defined here in Terraform|https://github.ucds.io/dip/ucfs-claimant/blob/2cd34ffadb28b29f5a209f9373e910bffe81c727/kms.tf#L94]
  * KMS arn for the CMK should be set in an env var
  * existing encryption object is formed in [this line of code|https://git.ucd.gpn.gov.uk/dip/ucfs-claimant-etl/blob/master/pb/transform.go#L109]
  * {{encryptedTakeHomePay}} is an object of this structure:

    {
        takeHomePay: <take home pay float, encrypted with the data key>
        cipherTextBlob: <encrypted data key that encrypted the take home pay>
        keyId: <the arn of the CMK that encrypted the data key>
    }

* the transformed message is safe to be logged, so log at INFO level. (!) The only unsafe element is the unhashed NINO, which must never be logged
