# Prompt 09: Video Assembly Service

Implement the video assembly service that renders the final MP4 output.

Requirements:
- Create a service that uses FFmpeg to combine audio, visual clips, and subtitles.
- Assemble the final video based on scene ordering and timing.
- Support basic subtitle burn-in into the output video.
- Create final MP4 output and return the file path.
- Handle common FFmpeg errors with clear messages.

Deliverables:
- FFmpeg-based assembly service
- final MP4 file generation
- error handling for assembly failures

Constraints:
- Keep the implementation focused on MVP functionality.
- Make the FFmpeg command structure easy to maintain.
