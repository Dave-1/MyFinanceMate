#!/bin/bash
# validate-fdroid-metadata.sh
# Validates F-Droid metadata against the fdroiddata schema locally
# before pushing to GitLab. Prevents pipeline failures.
#
# Usage: ./scripts/validate-fdroid-metadata.sh
# Requires: Python 3, pip install check-jsonschema

set -euo pipefail

METADATA_FILE="metadata/com.myfinancemate.yml"
SCHEMA_URL="https://gitlab.com/fdroid/fdroiddata/-/raw/master/schemas/metadata.json"
SCHEMA_CACHE="/tmp/fdroid-metadata-schema.json"
TMPDIR_VALIDATION=$(mktemp -d)

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

cleanup() {
    rm -rf "$TMPDIR_VALIDATION"
}
trap cleanup EXIT

echo "=== F-Droid Metadata Validator ==="
echo ""

# 1. Check that metadata file exists
if [ ! -f "$METADATA_FILE" ]; then
    echo -e "${RED}ERROR: $METADATA_FILE not found${NC}"
    exit 1
fi
echo -e "${GREEN}[OK]${NC} Metadata file exists: $METADATA_FILE"

# 2. Install check-jsonschema if not available
if ! command -v check-jsonschema &> /dev/null; then
    echo -e "${YELLOW}Installing check-jsonschema...${NC}"
    pip install check-jsonschema --quiet
fi
echo -e "${GREEN}[OK]${NC} check-jsonschema is available"

# 3. Download/cache the schema
if [ ! -f "$SCHEMA_CACHE" ]; then
    echo "Downloading F-Droid metadata schema..."
    curl -sL "$SCHEMA_URL" -o "$SCHEMA_CACHE"
fi
echo -e "${GREEN}[OK]${NC} Schema loaded"

# 4. Run schema validation
echo ""
echo "Running schema validation..."
if check-jsonschema --schemafile "$SCHEMA_CACHE" "$METADATA_FILE"; then
    echo -e "${GREEN}[OK]${NC} Schema validation passed"
else
    echo -e "${RED}[FAIL]${NC} Schema validation failed"
    exit 1
fi

# 5. Check required fields exist
echo ""
echo "Checking required fields..."

required_fields=("Categories" "License" "Builds" "AutoUpdateMode" "UpdateCheckMode" "CurrentVersion" "CurrentVersionCode" "Repo" "RepoType")
missing=0

for field in "${required_fields[@]}"; do
    if grep -q "^${field}:" "$METADATA_FILE"; then
        echo -e "${GREEN}[OK]${NC} $field"
    else
        echo -e "${RED}[MISSING]${NC} $field"
        missing=1
    fi
done

if [ $missing -eq 1 ]; then
    echo -e "${RED}Missing required fields!${NC}"
    exit 1
fi

# 6. Check AllowedAPKSigningKeys is lowercase hex (if present)
echo ""
echo "Checking AllowedAPKSigningKeys format..."
if grep -q "^AllowedAPKSigningKeys:" "$METADATA_FILE"; then
    SIGNING_KEY=$(grep "^AllowedAPKSigningKeys:" "$METADATA_FILE" | sed 's/AllowedAPKSigningKeys: *//')
    if echo "$SIGNING_KEY" | grep -qE '^[0-9a-f]{64}$'; then
        echo -e "${GREEN}[OK]${NC} AllowedAPKSigningKeys is valid lowercase hex (64 chars)"
    else
        echo -e "${RED}[FAIL]${NC} AllowedAPKSigningKeys must be lowercase hex (0-9a-f), 64 characters"
        echo "  Got: $SIGNING_KEY"
        exit 1
    fi
else
    echo -e "${YELLOW}[SKIP]${NC} AllowedAPKSigningKeys not set (optional, needed for reproducible builds)"
fi

# 7. Check Binaries URL format (if present)
echo ""
echo "Checking Binaries URL format..."
if grep -q "^Binaries:" "$METADATA_FILE"; then
    BINARIES_LINE=$(grep -A1 "^Binaries:" "$METADATA_FILE" | tail -1 | sed 's/^ *//')
    if echo "$BINARIES_LINE" | grep -qE '^https://.*%v.*$'; then
        echo -e "${GREEN}[OK]${NC} Binaries URL contains %v placeholder"
    else
        echo -e "${RED}[FAIL]${NC} Binaries URL must be HTTPS and contain %v"
        echo "  Got: $BINARIES_LINE"
        exit 1
    fi
else
    echo -e "${YELLOW}[SKIP]${NC} Binaries not set (optional, needed for reproducible builds)"
fi

# 8. Check commit hash is 40 hex chars
echo ""
echo "Checking commit hash format..."
COMMIT_HASH=$(grep "commit:" "$METADATA_FILE" | sed 's/.*commit: *//')
if echo "$COMMIT_HASH" | grep -qE '^[0-9a-f]{40}$'; then
    echo -e "${GREEN}[OK]${NC} Commit hash is valid (40 hex chars): ${COMMIT_HASH:0:12}..."
else
    echo -e "${RED}[FAIL]${NC} Commit hash must be 40 hex characters"
    echo "  Got: $COMMIT_HASH"
    exit 1
fi

# 9. Check Binaries comes before Builds (rewritemeta format, if Binaries present)
echo ""
echo "Checking field ordering (rewritemeta format)..."
if grep -q "^Binaries:" "$METADATA_FILE"; then
    BINARIES_LINE_NUM=$(grep -n "^Binaries:" "$METADATA_FILE" | head -1 | cut -d: -f1)
    BUILDS_LINE_NUM=$(grep -n "^Builds:" "$METADATA_FILE" | head -1 | cut -d: -f1)
    if [ "$BINARIES_LINE_NUM" -lt "$BUILDS_LINE_NUM" ]; then
        echo -e "${GREEN}[OK]${NC} Binaries (line $BINARIES_LINE_NUM) comes before Builds (line $BUILDS_LINE_NUM)"
    else
        echo -e "${RED}[FAIL]${NC} Binaries must come before Builds for fdroid rewritemeta compatibility"
        exit 1
    fi
else
    echo -e "${YELLOW}[SKIP]${NC} Binaries not set, skipping ordering check"
fi

# 10. Check Binaries is multi-line format with trailing space (rewritemeta format)
echo ""
echo "Checking Binaries multi-line format..."
if grep -q "^Binaries: $" "$METADATA_FILE" && grep -A1 "^Binaries: $" "$METADATA_FILE" | grep -qE '^  https://'; then
    echo -e "${GREEN}[OK]${NC} Binaries uses multi-line format with trailing space"
elif grep -q "^Binaries:" "$METADATA_FILE"; then
    echo -e "${RED}[FAIL]${NC} Binaries must use: 'Binaries: ' (with trailing space) followed by indented URL"
    exit 1
else
    echo -e "${YELLOW}[SKIP]${NC} Binaries not set, skipping format check"
fi

echo ""
echo -e "${GREEN}=== All validations passed! ===${NC}"
echo "Safe to push to GitLab."
