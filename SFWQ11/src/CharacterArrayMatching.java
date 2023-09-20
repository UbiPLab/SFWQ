public class CharacterArrayMatching {
    public static boolean matchArrays(char[] A, int[] C, char[] B) {
        int groupIndexA = 0;
        int groupIndexB = 0;
        boolean F=false;
        for (int i = 0; i < C.length; i++) {

            int groupLength = C[i];

            // Find the first character in A group in B
            int foundIndex = findCharInArray(B, A[groupIndexA], groupIndexB);
            if (foundIndex == -1 || 0>B.length - groupLength) {
                return false; // Character not found in B or B is out of bounds, match failed
            }

            // Check if the subsequent characters in A group match the corresponding characters in B
            for (int j = 1; j < groupLength; j++) {
                char currentCharA = A[groupIndexA + j];
                char currentCharB = B[foundIndex + j];
                if (currentCharA != currentCharB) {
                    F=true;
                    break; // Characters don't match, match failed
                }
            }
            if(F){
                i=i-1;
                groupIndexB = foundIndex+1;
                F=false;
            }
            else {
                groupIndexA += groupLength;
                groupIndexB = foundIndex + groupLength;
            }
        }

        return true; // All characters matched successfully
    }

    private static int findCharInArray(char[] arr, char target, int startIndex) {
        for (int i = startIndex; i < arr.length; i++) {
            if (arr[i] == target) {
                return i; // Character found at index i
            }
        }
        return -1; // Character not found
    }

    public static void main(String[] args) {
        char[] A = {'h', 'e', 'l', 'l', 'o', 'w', 'o', 'r', 'l', 'd','p'};
        int[] C = {5,5,1};
        char[] B = {'h', 'e', 'l', 'l', 'o', 'w', 'o', 'r', 'l', 'd'};

        boolean isSuccess = matchArrays(A, C, B);
        if (isSuccess) {
            System.out.println("Success"); // All characters matched successfully
        } else {
            System.out.println("Failure"); // Match failed
        }
    }
}
