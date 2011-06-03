fid = fopen('java','w');
for i=55:-1:1
    fprintf(fid,'\t\ttempDrawable.put(-%.1ff, R.drawable.t_%d);\n',i,i);
end
for i=0:1:50
    fprintf(fid,'\t\ttempDrawable.put(%.1ff, R.drawable.t%d);\n',i,i);
end
fclose(fid);