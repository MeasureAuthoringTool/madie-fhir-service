library FluentFunction version '1.0.000'

include FHIRHelpers version '4.0.001' called FHIRHelpers
valueset "Office Visit": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1001'
valueset "Annual Wellness Visit": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.526.3.1240'
valueset "Preventive Care Services - Established Office Visit, 18 and Up": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1025'
valueset "Preventive Care Services-Initial Office Visit, 18 and Up": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1023'
valueset "Home Healthcare Services": 'http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1016'
parameter "Measurement Period" Interval<DateTime>
 default Interval[@2019-01-01T00:00:00.0, @2020-01-01T00:00:00.0)
 context Patient
 define "Initial Population":
   exists "Qualifying Encounters"
   define "Qualifying Encounters":
       (
          [Encounter: "Office Visit"]
                   union [Encounter: "Annual Wellness Visit"]
                            union [Encounter: "Preventive Care Services - Established Office Visit, 18 and Up"]
                                     union [Encounter: "Preventive Care Services-Initial Office Visit, 18 and Up"]
                                              union [Encounter: "Home Healthcare Services"]
                                               ) ValidEncounter
                                                       where ValidEncounter.period during "Measurement Period"
                                                                 and ValidEncounter.isFinishedEncounter()
                                                                 
define fluent function "isFinishedEncounter"(Enc Encounter):
  (Enc E where E.status = 'finished') is not null 

