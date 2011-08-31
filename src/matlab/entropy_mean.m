function entropy_mean(file_list, images_path, out_file)

fid = fopen (file_list);
trainset = textscan(fid,'%s');
fclose(fid);

fout = fopen (out_file, 'w');

[trainSize, b] = size(trainset{1});
for i = 1:trainSize
    filename = strcat(images_path, trainset{1}{i}, '.jpg');
    x = imread(filename);
    [h, w, dim] = size(x);
    if dim ~= 3
        fprintf (fout, '%s,%f\n', trainset{1}{i},0.0);
        continue;
    end;
    hsv = rgb2hsv(x);
    j = entropyfilt(hsv);
    d = j(:,:,1)./j(:,:,3);
    
    for k = 1:h
        for l = 1:w
            if d(k,l) == Inf
                d(k,l) = 0;
            end;
        end;
    end;
    meanOfPic = nanmean(nanmean(d));
    fprintf (fout, '%s,%f\n', trainset{1}{i},meanOfPic);
end

fclose (fout);
