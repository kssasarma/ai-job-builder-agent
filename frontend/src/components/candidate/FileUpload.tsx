import { useCallback, useState } from "react";
import { useDropzone } from "react-dropzone";
import { UploadCloud, File, X, CheckCircle2 } from "lucide-react";
import { Button } from "../ui/button";
import { cn } from "../../lib/utils";

interface FileUploadProps {
  onUpload: (file: File) => void;
  loading: boolean;
  className?: string;
}

export function FileUpload({ onUpload, loading, className }: FileUploadProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const onDrop = useCallback((acceptedFiles: File[]) => {
    if (acceptedFiles.length > 0) {
      setSelectedFile(acceptedFiles[0]);
    }
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: { "application/pdf": [".pdf"] },
    maxFiles: 1,
    maxSize: 5 * 1024 * 1024, // 5MB
    disabled: loading,
  });

  const handleUpload = () => {
    if (selectedFile) {
      onUpload(selectedFile);
    }
  };

  const removeFile = (e: React.MouseEvent) => {
    e.stopPropagation();
    setSelectedFile(null);
  };

  return (
    <div className={cn("w-full space-y-4", className)}>
      <div
        {...getRootProps()}
        className={cn(
          "border-2 border-dashed rounded-lg p-8 transition-colors text-center cursor-pointer flex flex-col items-center justify-center space-y-3",
          isDragActive ? "border-primary bg-primary/5" : "border-muted-foreground/25 hover:border-primary/50",
          loading ? "opacity-50 cursor-not-allowed" : "",
          selectedFile ? "bg-muted/50 border-solid" : ""
        )}
      >
        <input {...getInputProps()} />

        {selectedFile ? (
          <div className="flex flex-col items-center space-y-2">
            <File className="h-10 w-10 text-primary" />
            <div className="flex items-center space-x-2">
              <span className="text-sm font-medium">{selectedFile.name}</span>
              <button onClick={removeFile} className="p-1 hover:bg-destructive/10 rounded-full transition-colors">
                <X className="h-4 w-4 text-destructive" />
              </button>
            </div>
            <span className="text-xs text-muted-foreground">
              {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
            </span>
          </div>
        ) : (
          <>
            <div className="p-3 bg-primary/10 rounded-full">
              <UploadCloud className="h-8 w-8 text-primary" />
            </div>
            <div>
              <p className="text-sm font-medium">Click to upload or drag and drop</p>
              <p className="text-xs text-muted-foreground mt-1">PDF only (max. 5MB)</p>
            </div>
          </>
        )}
      </div>

      <Button
        className="w-full"
        disabled={!selectedFile || loading}
        onClick={handleUpload}
      >
        {loading ? (
          "Uploading..."
        ) : (
          <>
            <CheckCircle2 className="mr-2 h-4 w-4" />
            Score My Resume
          </>
        )}
      </Button>
    </div>
  );
}
