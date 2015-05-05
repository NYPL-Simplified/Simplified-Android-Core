package org.nypl.simplified.downloader.core;

import com.io7m.junreachable.UnreachableCodeException;

public enum DownloadStatus
{
  STATUS_CANCELLED,
  STATUS_COMPLETED_NOT_TAKEN,
  STATUS_COMPLETED_TAKEN,
  STATUS_FAILED,
  STATUS_IN_PROGRESS,
  STATUS_IN_PROGRESS_RESUMED,
  STATUS_PAUSED;

  public boolean isComplete()
  {
    switch (this) {
      case STATUS_CANCELLED:
      case STATUS_COMPLETED_NOT_TAKEN:
      case STATUS_COMPLETED_TAKEN:
      case STATUS_FAILED:
        return true;
      case STATUS_IN_PROGRESS:
      case STATUS_IN_PROGRESS_RESUMED:
      case STATUS_PAUSED:
        return false;
    }

    throw new UnreachableCodeException();
  }

  public boolean isResumable()
  {
    switch (this) {
      case STATUS_CANCELLED:
      case STATUS_COMPLETED_NOT_TAKEN:
      case STATUS_COMPLETED_TAKEN:
      case STATUS_FAILED:
        return false;
      case STATUS_IN_PROGRESS:
      case STATUS_IN_PROGRESS_RESUMED:
      case STATUS_PAUSED:
        return true;
    }

    throw new UnreachableCodeException();
  }
}
