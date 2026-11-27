package com.mayureshpatel.pfdataservice.dto.merchant;

import java.util.List;

/**
 * One group of selected descriptions that carries words the target merchant's name lacks, where
 * those words point somewhere else (PF-983). The page shows it as, for example, "4 of these
 * contain FUEL, which Kroger doesn't. Kroger Fuel Center exists."
 *
 * @param groupName      the display name of the descriptions' group ("KROGER FUEL CTR")
 * @param words          the flagged words, uppercase, in the order they appear in the group name
 * @param descriptions   the selected raw descriptions in this group, as sent
 * @param merchants      the user's other merchants that have those words and fit the group better
 *                       than the target does, best first; may be empty. Each merchant's
 *                       {@code linkCount} is not filled in and reads 0
 * @param nextToGroup    the display name of another selected group this one only adds words to
 *                       ("KROGER" for "KROGER FUEL CTR"), or {@code null} when none is selected
 */
public record LookAlikeWarningDto(
        String groupName,
        List<String> words,
        List<String> descriptions,
        List<MerchantDto> merchants,
        String nextToGroup
) {
}
