# Turns a simulation-performance response (scripts/pts-simulation.sh) into one verdict per test task.
#
#   scripts/pts-simulation.sh <server> <project> | jq -r -f scripts/pts-readiness.jq
#
# The criteria are in references/simulator.md.

def num: if type == "number" then (if isnan then null else . end) else null end;
def pct: if num == null then "-" else (. * 1000 | round / 10 | tostring) + "%" end;
def duration: if num == null then "-" elif . < 60000 then (. / 1000 | round | tostring) + " s"
  else (. / 60000 * 10 | round / 10 | tostring) + " min" end;
def ratio(a; b): if b > 0 then a / b else null end;
def band(mean_ms):
  if mean_ms < 60000 then 0.99
  elif mean_ms < 300000 then 0.98
  elif mean_ms < 600000 then 0.95
  else 0.90 end;

(["project", "target", "builds", "mean", "task failures", "predicted", "need", "test failures predicted",
  "savings", "insufficient data", "verdict"] | @tsv),
(.content
  | sort_by(-.buildsWithPTSSimulated)[]
  | .serialTimeSavingsPercent |= num
  | .meanExecutionDurationMs |= (num // 0)
  | (.serialTimeSavings + .unavoidableTime + .insufficientDataTime) as $total
  | ratio(.selectedTaskFailures; .totalTaskFailures) as $taskPredicted
  | ratio(.insufficientDataTime; $total) as $insufficient
  | band(.meanExecutionDurationMs) as $need
  | (if .buildsWithPTSSimulated == 0 then "not yet: no simulated builds"
     elif .targetExecutionsWithInputCaptureIssues > 0 then "blocked: input capture issues in \(.targetExecutionsWithInputCaptureIssues) runs"
     elif ($insufficient // 1) > 0.10 then "not yet: too much insufficient data"
     elif .totalTaskFailures == 0 then "not yet: no failures in the window, so prediction quality is unknown"
     elif $taskPredicted < $need then "review: task failures predicted below \($need | pct)"
     elif (.serialTimeSavingsPercent // 0) < 0.10 then "ready, little to gain"
     else "ready" end) as $verdict
  | [.project, .target, .buildsWithPTSSimulated, (.meanExecutionDurationMs | duration),
     "\(.selectedTaskFailures)/\(.totalTaskFailures)", ($taskPredicted | pct), ($need | pct),
     (ratio(.selectedClassFailures; .totalClassFailures) | pct),
     (.serialTimeSavingsPercent | pct), ($insufficient | pct), $verdict]
  | @tsv)
