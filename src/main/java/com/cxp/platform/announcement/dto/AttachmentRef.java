package com.cxp.platform.announcement.dto;

import java.util.UUID;

/**
 * Placeholder for a future media attachment on a civic announcement.
 * Cloudflare upload, signed URL generation, and media storage are out of scope until
 * the media-management sprint. This record establishes the contract shape only.
 */
public record AttachmentRef(UUID id, String type, String url) {}
