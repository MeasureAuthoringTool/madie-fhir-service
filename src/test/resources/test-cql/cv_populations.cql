library TestCVPopulations version '0.0.004'

using FHIR version '4.0.1'

include "FHIRHelpers" version '4.0.001' called FHIRHelpers
include SupplementalDataElementsFHIR4 version '2.0.000' called SDE
include MATGlobalCommonFunctionsFHIR4 version '5.0.000' called Global

codesystem "LOINC": 'http://loinc.org'

valueset "Competing Conditions for Respiratory Conditions": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.102.12.1017'
valueset "Encounter Inpatient": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.666.5.307'
valueset "Level of Severity of Retinopathy Findings": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.526.3.1283'

code "Functional Assessment of Chronic Illness Therapy - Palliative Care Questionnaire (FACIT-Pal)": '71007-9' from "LOINC" display 'Functional Assessment of Chronic Illness Therapy - Palliative Care Questionnaire (FACIT-Pal)'

parameter "Measurement Period" Interval<DateTime>

context Patient

define "SDE Ethnicity":
  SDE."SDE Ethnicity"

define "SDE Payer":
  SDE."SDE Payer"

define "SDE Race":
  SDE."SDE Race"

define "Initial Population":
  ["Encounter": "Encounter Inpatient"] Enc
                  where Enc.period ends during "Measurement Period"

define "Measure Population":
  "Initial Population"

define "Measure Population Exclusions":
  "Initial Population" Pop
                  where exists ( Pop.diagnosis diag
                      where diag.rank = 1
                  )

define "Valid Encounter":
  ["Encounter": "Encounter Inpatient"] Encounter
                  where Encounter.period during "Measurement Period"
                    and Encounter.length >= 24 hours

define function "Measure Observation"(Enc "Encounter" ):
  Count("Valid Encounter")


define "Unused Boolean Definition":
  exists ["Communication": "Level of Severity of Retinopathy Findings"] Communication
          where Communication.sent in "Measurement Period"