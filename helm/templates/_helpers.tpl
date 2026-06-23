{{- define "firefly.fullname" -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "firefly.redisName" -}}
{{- include "firefly.fullname" . }}-redis
{{- end }}

{{- define "firefly.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{ include "firefly.selectorLabels" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- with .Values.extraLabels }}
{{ toYaml . | trimSuffix "\n" }}
{{- end }}
{{- end }}

{{- define "firefly.selectorLabels" -}}
app.kubernetes.io/name: {{ .Chart.Name }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Returns "true" when running in multi-replica mode — either replicaCount > 1 or
autoscaling is enabled. Used to gate Redis deployment, session affinity annotations,
and the Redis host env var.
*/}}
{{- define "firefly.multiReplica" -}}
{{- if or (gt (int .Values.replicaCount) 1) .Values.autoscaling.enabled }}true{{- end }}
{{- end }}

{{/*
Validates chart values. Call this from deployment.yaml to catch misconfigurations early.
*/}}
{{- define "firefly.validate" -}}
{{- if and (include "firefly.multiReplica" .) (not .Values.persistence.sharedWorkDir.pvc) (not .Values.persistence.sharedWorkDir.existingClaim) }}
{{- fail "persistence.sharedWorkDir must be configured (pvc or existingClaim) in multi-replica mode (replicaCount > 1 or autoscaling.enabled). Use ReadWriteMany accessMode." }}
{{- end }}
{{- end }}
