function mean_values(dataset,output)

fid = fopen (dataset);
trainset = textscan(fid,'%s');
fclose(fid);

fout = fopen (output, 'w');

[setSize, b] = size(trainset{1});
for i = 1:setSize
	filename = strcat(images_path, trainset{1}{i}, '.jpg');
	x = imread(filename);
	meanOfPic = mean(mean(mean(x)));
	if meanOfPic > 200
		fprintf (fout, '%s,%d,%f\n', trainset{1}{i},1,meanOfPic);
	else 
		fprintf (fout, '%s,%d,%f\n', trainset{1}{i}, 0, meanOfPic);
	end
end

fclose (fout);
