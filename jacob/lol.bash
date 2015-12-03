for run in {1..20}
do
  appletviewer -J"-Djava.security.policy=all.policy" *.html &
done
