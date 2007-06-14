CREATE TABLE version_workload_history (versionID BIGINT, remainingTime INT, totalTime INT, remainingIssues INT, totalIssues INT, time TIMESTAMP, type INT DEFAULT NULL)
CREATE INDEX versionTypeIndex ON version_workload_history (versionID, type)
