import re
import os
import time
# Google API Client Library imports
from googleapiclient.discovery import build
from google.oauth2.service_account import Credentials
from googleapiclient.errors import HttpError

# --- Configuration ---
# !!! UPDATE THESE VALUES BELOW !!!
# Scopes required for Sheets API v4
SCOPES = ['https://www.googleapis.com/auth/spreadsheets']
SERVICE_ACCOUNT_FILE = '/home/drking/Downloads/rugged-lacing-457309-m4-25cbf2200994.json' # <--- UPDATE THIS
SPREADSHEET_ID = '1wBOu4WjVvNzjIGPKWyhLUum3aA_JpSD_8a89d-cfnBo'                  # <--- UPDATE THIS
SHEET_NAME = 'Elki-composite'                         # <--- UPDATE THIS (Exact tab name)
# Path to the SINGLE input text file containing all outputs
INPUT_FILE_PATH = '/home/drking/Documents/bakalarka/data/eval_output_64_only.txt'       # <--- UPDATE THIS
# Path to the single TXT file with all outputs
TARGET_K_VALUE = 'k=4' # The label for the row we care about in Sheet Col A

# Regex to parse parameters from the filename inside the TXT file
filename_param_pattern = re.compile(r"Elki_(\d+)_([\d\.]+)_(\d+)\.composite")

# --- END OF CONFIGURATION ---

# Regex to extract the k=4 classification precision value
precision_pattern = re.compile(
    r"Search evaluation \(k=4\):.*?"
    r"Classifying\.\.\..*?"
    r"classification precision over objects and categories:\s*(\d+\.\d+)",
    re.DOTALL
)

# Regex to extract the full file path from the "NEW EXPERIMENT" line
experiment_header_pattern = re.compile(r"===== NEW EXPERIMENT: (.*?) =====")


# --- Helper Function to Find Cell (using google-api-python-client) ---
def find_target_cell(service, param1, param2, param3, target_k):
    """Finds target row and column using direct API calls."""
    try:
        # Define ranges for headers and k-column
        # ASSUMPTION: K values in Col A, Param1 in Row 1, Param2 in Row 2, Param3 in Row 3
        # !!! Adjust sheet name and ranges if assumptions are wrong !!!
        ranges_to_get = [
            f"{SHEET_NAME}!A:A", # K values column
            f"{SHEET_NAME}!2:2", # Param1 header row
            f"{SHEET_NAME}!3:3", # Param2 header row
            f"{SHEET_NAME}!4:4"  # Param3 header row
        ]
        # Make API call to get all ranges at once
        result = service.spreadsheets().values().batchGet(
            spreadsheetId=SPREADSHEET_ID,
            ranges=ranges_to_get
        ).execute()

        value_ranges = result.get('valueRanges', [])
        if len(value_ranges) != 4:
            print("    Error: Did not receive expected data ranges from Sheet API.")
            return None, None

        # Extract values, handling potential missing 'values' key if range is empty
        k_col_values_nested = value_ranges[0].get('values', [])
        header_row_param1_nested = value_ranges[1].get('values', [[]]) # Ensure at least [[]]
        header_row_param2_nested = value_ranges[2].get('values', [[]]) # Ensure at least [[]]
        header_row_param3_nested = value_ranges[3].get('values', [[]]) # Ensure at least [[]]

        # API returns nested lists, e.g., [['k=4'], ['k=1']]; [['256', '', '']]
        # Flatten k column values
        k_col_values = [item[0] if item else "" for item in k_col_values_nested]
        # Get first row (and only row) from header results
        header_row_param1 = header_row_param1_nested[0]
        header_row_param2 = header_row_param2_nested[0]
        header_row_param3 = header_row_param3_nested[0]

        # --- Find Target Row ---
        target_row_index = -1
        for i, val in enumerate(k_col_values):
            if str(val).strip() == target_k:
                target_row_index = i + 1 # 1-based index for sheets
                break
        if target_row_index == -1:
            print(f"    Error: Row for '{target_k}' not found in Column A.")
            return None, None

        # --- Find Target Column (Nested Search) ---
        col_range_p1_start, col_range_p1_end = -1, -1
        col_range_p2_start, col_range_p2_end = -1, -1
        target_col_index = -1
        current_param1 = None

        # 1. Find Column Range for Param1 in Row 1
        for i, val in enumerate(header_row_param1):
            cell_val = str(val).strip()
            if cell_val != "": current_param1 = cell_val
            if current_param1 == str(param1):
                if col_range_p1_start == -1: col_range_p1_start = i + 1
                col_range_p1_end = i + 1
            elif col_range_p1_start != -1: break

        if col_range_p1_start == -1:
            print(f"    Error: Header for Param1='{param1}' not found in Row 1.")
            return None, None

        # 2. Find Column Range for Param2 in Row 2 *within Param1's range*
        current_param2 = None
        # Pad row list in case it's shorter than expected indices
        header_row_param2.extend([''] * (col_range_p1_end - len(header_row_param2)))
        for i in range(col_range_p1_start - 1, col_range_p1_end):
            cell_val = str(header_row_param2[i]).strip()
            if cell_val != "": current_param2 = cell_val
            if current_param2 == str(param2):
                 if col_range_p2_start == -1: col_range_p2_start = i + 1
                 col_range_p2_end = i + 1
            elif col_range_p2_start != -1: break

        if col_range_p2_start == -1:
            print(f"    Error: Header for Param2='{param2}' not found under Param1='{param1}' in Row 2 (Cols {col_range_p1_start}-{col_range_p1_end}).")
            return None, None

        # 3. Find Specific Column for Param3 in Row 3 *within Param2's range*
        # Pad row list
        header_row_param3.extend([''] * (col_range_p2_end - len(header_row_param3)))
        for i in range(col_range_p2_start - 1, col_range_p2_end):
             cell_val = str(header_row_param3[i]).strip()
             if cell_val == str(param3):
                 target_col_index = i + 1
                 break

        if target_col_index == -1:
            print(f"    Error: Header for Param3='{param3}' not found under Param2='{param2}' (within Param1='{param1}') in Row 3 (Cols {col_range_p2_start}-{col_range_p2_end}).")
            return None, None

        return target_row_index, target_col_index

    except HttpError as e:
        print(f"    Google API HTTP Error finding cell coordinates: {e}")
        error_details = e.resp.get('content', '{}')
        print(f"    Error details: {error_details}")
        return None, None
    except Exception as e:
        print(f"    Error finding cell coordinates: {e}")
        return None, None

# --- Main Execution ---
def main():
    # --- Authenticate and build Sheets API service ---
    creds = None
    try:
        print("Authenticating using service account...")
        creds = Credentials.from_service_account_file(
            SERVICE_ACCOUNT_FILE, scopes=SCOPES)
        # Build the service object
        service = build('sheets', 'v4', credentials=creds)
        print("Google Sheets API service built successfully.")
    except FileNotFoundError:
        print(f"Error: Service account file not found at '{SERVICE_ACCOUNT_FILE}'")
        return
    except Exception as e:
        print(f"Error during authentication or building service: {e}")
        return

    # --- Read the single input file ---
    try:
        print(f"Reading input file: '{INPUT_FILE_PATH}'")
        with open(INPUT_FILE_PATH, 'r', encoding='utf-8') as f:
            full_content = f.read()
    except FileNotFoundError:
         print(f"Error: Input data file not found at '{INPUT_FILE_PATH}'")
         return
    except Exception as e:
        print(f"Error reading input file '{INPUT_FILE_PATH}': {e}")
        return

    # --- Split content into experiment blocks ---
    split_pattern = r"(===== NEW EXPERIMENT:.*?=====)"
    parts = re.split(split_pattern, full_content)
    experiment_blocks = []
    for i in range(1, len(parts), 2):
        delimiter = parts[i]
        block_text = parts[i+1] if (i+1) < len(parts) else ""
        experiment_blocks.append(delimiter + block_text)

    if not experiment_blocks:
        print("Error: No '===== NEW EXPERIMENT:' markers found in the input file.")
        return
    print(f"Found {len(experiment_blocks)} experiment blocks to process.")

    # --- Process each block ---
    updates_count = 0
    api_errors = 0
    for i, block_content in enumerate(experiment_blocks):
        print(f"\n--- Processing Block {i+1}/{len(experiment_blocks)} ---")

        header_match = experiment_header_pattern.search(block_content)
        if not header_match:
            print("  Warning: Could not find 'NEW EXPERIMENT' header. Skipping.")
            continue
        file_path = header_match.group(1).strip()
        basename = os.path.basename(file_path)
        print(f"  Filename: {basename}")

        param_match = filename_param_pattern.match(basename)
        if not param_match:
            print(f"  Warning: Could not parse parameters from filename. Skipping.")
            continue
        param1, param2, param3 = param_match.groups()
        print(f"  Params: P1='{param1}', P2='{param2}', P3='{param3}'")

        precision_match = precision_pattern.search(block_content)
        if not precision_match:
            print(f"  Warning: '{TARGET_K_VALUE}' precision pattern not found. Skipping.")
            continue
        precision_value = float(precision_match.group(1))
        print(f"  Precision: {precision_value}")

        # Find target cell using the direct API helper function
        target_row, target_col = find_target_cell(service, param1, param2, param3, TARGET_K_VALUE)

        if target_row and target_col:
            # Use RC notation which is easier than converting Col number to letter
            target_range_rc = f"{SHEET_NAME}!R{target_row}C{target_col}"
            print(f"  Target Cell: {target_range_rc}")

            # Prepare body for API update call
            update_body = {
                'values': [[precision_value]]  # Value must be nested in two lists
            }
            try:
                # Make the API call to update the single cell
                result = service.spreadsheets().values().update(
                    spreadsheetId=SPREADSHEET_ID,
                    range=target_range_rc,
                    valueInputOption='USER_ENTERED', # Treats input like user typing it
                    body=update_body).execute()
                updates_count += 1
                print(f"  Success: Updated cell.") # Result: {result.get('updatedCells')}")
                time.sleep(1.1) # API rate limit delay

            except HttpError as e:
                 api_errors += 1
                 print(f"  Google API HTTP Error updating cell: {e}")
                 error_details = e.resp.get('content', '{}')
                 print(f"  Error details: {error_details}")
                 print("      Check range notation, permissions, and API quotas.")
                 time.sleep(5) # Longer sleep after an API error
            except Exception as e:
                 print(f"  Error updating cell: {e}")
        else:
            print(f"  Warning: Could not find target cell in sheet for these parameters.")

    print(f"\n--- Processing complete ---")
    print(f"Successfully updated {updates_count} cells.")
    if api_errors > 0:
        print(f"Encountered {api_errors} API errors during updates.")

if __name__ == '__main__':
    main()